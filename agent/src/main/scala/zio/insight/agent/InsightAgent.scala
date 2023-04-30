package zio.insight.agent

import com.google.protobuf.ByteString

import zio._
import zio.insight.api._
import zio.insight.api.protocol.InsightMessage.InsightMessage
import zio.stream.ZStream

trait InsightAgent {
  def applicationId: String
  def registerPlugin(plugin: InsightPlugin): ZIO[Any, InsightError, Unit]
  def sendMsg(plugin: InsightPlugin)(msg: plugin.Message): ZIO[Any, InsightError, Unit]
  def subscribe(): ZIO[Scope, Nothing, ZStream[Any, Nothing, InsightMessage]]
  def handle(msg: InsightMessage): ZIO[Any, InsightError, Unit]
}

object InsightAgent {

  private def make: ZIO[Any, Nothing, InsightAgent] = {
    val appIdLen           = 8
    val chars: Chunk[Char] =
      Chunk.fromIterable('a'.to('z')) ++ Chunk.fromIterable('A'.to('Z')) ++ Chunk.fromIterable('0'.to('9'))

    def rndChar = ZIO.randomWith(rnd => rnd.nextIntBounded(chars.length).map(chars(_)))
    def genId   = ZIO.foreachPar(1.to(appIdLen))(_ => rndChar).map(_.mkString)

    for {
      appId   <- genId
      plugins <- Ref.make(Map.empty[Int, InsightPlugin])
      msgHub  <- Hub.bounded[InsightMessage](1024)
    } yield new InsightAgentImpl(appId, plugins, msgHub) {}
  }

  sealed abstract private[agent] class InsightAgentImpl(
    override val applicationId: String,
    plugins: Ref[Map[Int, InsightPlugin]],
    msgHub: Hub[InsightMessage])
      extends InsightAgent {
    override def registerPlugin(plugin: InsightPlugin): ZIO[Any, Nothing, Unit] =
      plugins.update(_ + (plugin.pluginId -> plugin)).unit

    private def createMessage(plugin: InsightPlugin)(msg: plugin.Message): ZIO[Any, Nothing, InsightMessage] =
      for {
        // TODO: millis or nanos ?
        now <- ZIO.clockWith(_.nanoTime)
      } yield (
        InsightMessage(
          applicationId,
          protocol.InsightMessage.InsightPlugin.fromValue(plugin.pluginId),
          plugin.msgTag(msg),
          now,
          ByteString.copyFrom(plugin.encoder.encode(msg).toArray),
        ),
      )

    override def sendMsg(plugin: InsightPlugin)(msg: plugin.Message): ZIO[Any, InsightError, Unit] =
      for {
        msg <- createMessage(plugin)(msg)
        _   <- msgHub.publish(msg)
        _   <- ZIO.logInfo(s"Agent is processing message: ${msg.dump}")
      } yield ()

    override def subscribe(): ZIO[Scope, Nothing, ZStream[Any, Nothing, InsightMessage]] =
      ZStream.fromHubScoped(msgHub)

    override def handle(msg: InsightMessage): ZIO[Any, InsightError, Unit] = ZIO.unit
  }

  val live = ZLayer.fromZIO(make)
}
