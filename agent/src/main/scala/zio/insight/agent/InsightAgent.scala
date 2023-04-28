package zio.insight.agent

import zio._
import zio.insight.api._

trait InsightAgent {
  def applicationId: String
  def registerPlugin(plugin: InsightPlugin): ZIO[Any, InsightError, Unit]
  def sendMsg(): ZIO[Any, InsightError, Unit]
}

object InsightAgent {

  private def make: ZIO[Any, Nothing, InsightAgent] =
    ZIO.randomWith(rnd => rnd.nextString(5)).map(id => new InsightAgentImpl(id) {})

  sealed abstract private[agent] class InsightAgentImpl(
    override val applicationId: String)
      extends InsightAgent {
    override def registerPlugin(plugin: InsightPlugin): ZIO[Any, InsightError, Unit] = ZIO.unit
    override def sendMsg(): ZIO[Any, InsightError, Unit]                             = ZIO.unit
  }

  val live = ZLayer.fromZIO(make)
}
