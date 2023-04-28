package zio.insight.agent

import zio._

trait InsightAgent {
  def sendMsg(): ZIO[Any, InsightError, Unit]
}

object InsightAgent {}
