package zio.insight.plugins.agent

import zio.insight.agent.protocol.AgentMessages.{ApplicationStarted => ApplicationStartedProto}

sealed trait InsightAgentMessage {
  def msgId: Int
  def msgTag: String
}

object InsightAgentMessage {

  final case class ApplicationStarted(
    name: String,
    version: String)
      extends InsightAgentMessage {
    override val msgId          = ApplicationStarted.msgId
    override val msgTag: String = ApplicationStarted.msgTag
  }

  object ApplicationStarted {
    val msgTag = "Started"
    val msgId  = 0
  }

  implicit class InsightAgentMessageOps(val self: InsightAgentMessage) extends AnyVal {

    def toBytes: Array[Byte] = self match {
      case ApplicationStarted(name, version) => ApplicationStartedProto(name, version).toByteArray
    }
  }
}
