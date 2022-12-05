package sample

import zio._
import zio.insight.server.{insightServerLayer, InsightServerConfig}

object SampleApp extends ZIOAppDefault with InstrumentedSample {

  private val insightServerConfig = InsightServerConfig(8080, 5.seconds)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program
      .provide(
        insightServerLayer(insightServerConfig),
      )
}
