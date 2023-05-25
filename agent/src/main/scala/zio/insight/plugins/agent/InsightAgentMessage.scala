package zio.insight.plugins.agent

import zio.insight.agent.protocol.AgentMessages.{ApplicationStarted => ApplicationStartedProto}
import zio.insight.api.InsightPlugin.MsgTag

sealed trait InsightAgentMessage {
  def msgTag: MsgTag
}

object InsightAgentMessage {

  final case class ApplicationStarted(
    name: String,
    version: String)
      extends InsightAgentMessage {
    override val msgTag: String = ApplicationStarted.msgTag
  }

  object ApplicationStarted {
    val msgTag: MsgTag = "Started"
  }

  implicit class InsightAgentMessageOps(val self: InsightAgentMessage) extends AnyVal {

    def toBytes: Array[Byte] = self match {
      case ApplicationStarted(name, version) => ApplicationStartedProto(name, version).toByteArray
    }
  }
}
