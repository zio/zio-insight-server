package zio.insight.agent

import zio._
import zio.insight.api.InsightError
import zio.insight.api.protocol.InsightMessage.InsightMessage

trait AgentMessageHandler {
  def handleMessage(msg: InsightMessage): ZIO[Any, InsightError, Unit]
}
