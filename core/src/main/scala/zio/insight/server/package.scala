package zio.insight

import zio._
import zio.metrics.connectors.MetricsConfig

import zhttp.service.EventLoopGroup
import zhttp.service.server.ServerChannelFactory

package object server {
  private def insightServer(
  ): ZIO[InsightServerConfig with MetricsConfig with Scope, Throwable, Unit] =
    for {
      cfg        <- ZIO.service[InsightServerConfig]
      metricsCfg <- ZIO.service[MetricsConfig]
      svr        <- InsightServer
                      .run()
                      .provideSome(
                        EventLoopGroup.auto(cfg.nThreads),
                        ServerChannelFactory.auto,
                        ZLayer.succeed(cfg),
                        zio.metrics.connectors.insight.metricsLayer,
                      )
    } yield ()

  lazy val insightLayer = ZLayer.scoped(
    for {
      svr <- (insightServer() *> ZIO.never).forkScoped
    } yield (),
  )
}
