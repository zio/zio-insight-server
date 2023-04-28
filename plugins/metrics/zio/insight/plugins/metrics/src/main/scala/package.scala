package zio.metrics.connectors

import zio._
import zio.metrics.connectors.internal.MetricsClient

package object insight {

  private lazy val publisherLayer: ULayer[InsightPublisher] = ZLayer.fromZIO(InsightPublisher.make)

  lazy val metricsLayer: ZLayer[MetricsConfig, Nothing, InsightPublisher] =
    (publisherLayer ++ ZLayer.service[MetricsConfig]) >+> ZLayer.fromZIO(
      ZIO.service[InsightPublisher].flatMap(clt => MetricsClient.make(metricsHandler(clt))).unit,
    )

  private def metricsHandler(clt: InsightPublisher): Iterable[MetricEvent] => UIO[Unit] =
    events =>
      ZIO
        .foreach(events)(clt.update)
        .unit

}
