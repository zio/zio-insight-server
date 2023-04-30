package zio.insight.api

sealed trait InsightError {
  def msg: String
}

object InsightError {
  final case class InvalidTargetPlugin(expected: InsightPlugin, actual: InsightPlugin) extends InsightError {
    override def msg: String = s"Invalid target plugin. Expected: $expected, actual: $actual"
  }
}
