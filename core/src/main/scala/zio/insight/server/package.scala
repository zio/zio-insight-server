package zio.insight

import zio._
import zio.metrics.connectors.MetricsConfig
import zio.metrics.jvm.DefaultJvmMetrics

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
                        zio.metrics.connectors.insight.metricsLayer,
                        ZLayer.succeed(cfg),
                        ZLayer.succeed(metricsCfg),
                        // Enable the ZIO internal metrics and the default JVM metricsConfig
                        // Do NOT forget the .unit for the JVM metrics layer
                        Runtime.enableRuntimeMetrics,
                        Runtime.enableFiberRoots,
                        DefaultJvmMetrics.live.unit,
                      )
    } yield ()

  lazy val insightLayer = ZLayer.fromZIO(
    for {
      svr <- (insightServer() *> ZIO.never).forkDaemon
    } yield (),
  )
}
