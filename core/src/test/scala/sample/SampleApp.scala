package sample

import zio._
import zio.insight.server._
import zio.metrics.{Metric, MetricKeyType}
import zio.metrics.MetricLabel
import zio.metrics.connectors.MetricsConfig

object SampleApp extends ZIOAppDefault {

  // Create a histogram with 12 buckets: 0..100 in steps of 10, Infinite
  // It also can be applied to effects yielding a Double
  val aspHistogram =
    Metric.histogram("zmxHistogram", MetricKeyType.Histogram.Boundaries.linear(0.0d, 10.0d, 11))

  // Create a summary that can hold 100 samples, the max age of the samples is 1 day.
  // The summary should report th 10%, 50% and 90% Quantile
  // It can be applied to effects yielding an Int
  val aspSummary =
    Metric.summary("mySummary", 1.day, 100, 0.03d, Chunk(0.1, 0.5, 0.9)).contramap[Int](_.toDouble)

  // Create a Set to observe the occurrences of unique Strings
  // It can be applied to effects yielding a String
  val aspSet = Metric.frequency("mySet")

  // Create a counter applicable to any effect
  val aspCountAll = Metric.counter("countAll").contramap[Any](_ => 1L)

  private lazy val gaugeSomething = for {
    _ <- ZIO.foreachPar(0.to(9)) { idx =>
           Random.nextDoubleBetween(0, 100d).flatMap { v =>
             Metric.gauge("Gauge").tagged(MetricLabel("idx", s"$idx")).set(v) @@ aspCountAll
           }
         }
  } yield ()

  // Just record something into a histogram
  private lazy val observeNumbers = for {
    _ <- Random.nextDoubleBetween(0.0d, 120.0d) @@ aspHistogram @@ aspCountAll
    _ <- Random.nextIntBetween(100, 500) @@ aspSummary @@ aspCountAll
  } yield ()

  // Observe Strings in order to capture unique values
  private lazy val observeKey = for {
    _ <- Random.nextIntBetween(10, 20).map(v => s"myKey-$v") @@ aspSet @@ aspCountAll
  } yield ()

  private def program = for {
    _ <- gaugeSomething.schedule(Schedule.spaced(200.millis).jittered).forkScoped
    _ <- observeNumbers.schedule(Schedule.spaced(150.millis).jittered).forkScoped
    _ <- observeKey.schedule(Schedule.spaced(300.millis).jittered).forkScoped
  } yield ()

  override def run =
    (for {
      f <- ZIO.never.forkScoped
      _ <- program
      _ <- Console.printLine("Started Insight Sample application ...")
      _ <- f.join
    } yield ()).provideSome[Scope](
      // Update Metric State for the API endpoint every 5 seconds
      ZLayer.succeed(MetricsConfig(5.seconds)),
      // Configure the number of threads for ZIO HTTP and the port
      ZLayer.succeed(InsightServerConfig(5, 8080)),
      // Start ZIO HTTP with the embedded insight app
      insightLayer,
    )
}
