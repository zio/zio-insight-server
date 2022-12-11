package zio.insight.server

final case class InsightServerConfig(
  nThreads: Int,
  port: Int)
