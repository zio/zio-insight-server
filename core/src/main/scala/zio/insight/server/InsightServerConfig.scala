package zio.insight.server

import java.time.Duration

final case class InsightServerConfig(
  port: Int,
  interval: Duration)
