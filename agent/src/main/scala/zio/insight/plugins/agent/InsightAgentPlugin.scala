package zio.insight.plugins.agent

import zio._
import zio.insight.agent.InsightAgent
import zio.insight.api._

object InsightAgentPlugin {

  import InsightPlugin.MsgTag

  val pluginId = 0;

  sealed abstract private[agent] class InsightAgentPluginImpl(
    agent: InsightAgent)
      extends InsightPlugin { self: InsightPlugin =>
    override type Message = InsightAgentMessage

    override def msgTags: Chunk[MsgTag] = Chunk(
      InsightAgentMessage.ApplicationStarted.msgTag,
    )

    override def pluginId: Int = InsightAgentPlugin.pluginId

    override def msgTag(msg: Message): String = msg.msgTag

    override def decoder: InsightDecoder[Message] = ???
    override def encoder: InsightEncoder[Message] = new InsightEncoder[Message] {
      override def encode(a: Message): Chunk[Byte] = a match {
        case msg: InsightAgentMessage.ApplicationStarted =>
          Chunk.fromArray(msg.toBytes)
      }
    }

    override def send(msg: Message): ZIO[Any, InsightError, Unit] =
      agent.sendMsg(self)(msg)

    def handle(msg: Message): ZIO[Any, InsightError, Unit] = ZIO.unit
  }

  private def make =
    for {
      agent <- ZIO.service[InsightAgent]
      plugin = new InsightAgentPluginImpl(agent) {}
      _     <- agent.registerPlugin(plugin)
      _     <- plugin.send(InsightAgentMessage.ApplicationStarted("test", "0.0.1"))
    } yield plugin

  ZIO.service[InsightAgent].map(new InsightAgentPluginImpl(_) {})

  val live = ZLayer.fromZIO(make)

}
