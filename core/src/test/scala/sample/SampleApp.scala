package sample

import zio._
import zio.http._
import zio.insight.fiber._
import zio.insight.server.InsightServer
import zio.metrics._
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.insight
import zio.metrics.connectors.insight.InsightPublisher
import zio.metrics.jvm.DefaultJvmMetrics

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
    f1 <- (Random.nextDoubleBetween(0.0d, 120.0d) @@ aspHistogram @@ aspCountAll).forkScoped
    f2 <- (Random.nextIntBetween(100, 500) @@ aspSummary @@ aspCountAll).forkScoped
    d1 <- f1.join
    d2 <- f2.join
  } yield (d1, d2)

  private lazy val doSomething = {
    val t = (for {
      value <- ZIO.randomWith(_.nextIntBounded(5).map(_ + 1))
      _     <- ZIO.sleep((value * 100).millis)
      err   <- ZIO.randomWith(_.nextBoolean)
      _     <- ZIO.fail(new Exception("I dont like this")).when(err)
    } yield value).tapError(_ => ZIO.unit)

    for {
      f <- t.forkScoped
      r <- f.join
    } yield r
  }

  // Observe Strings in order to capture unique values
  private lazy val observeKey = for {
    _ <- (Random.nextIntBetween(10, 20).map(v => s"myKey-$v") @@ aspSet @@ aspCountAll).forkScoped
  } yield ()

  private def program = for {
    _ <- gaugeSomething.schedule(Schedule.spaced(500.millis).jittered).forkScoped
    _ <- observeNumbers.schedule(Schedule.spaced(400.millis).jittered).forkScoped
    _ <- observeKey.schedule(Schedule.spaced(300.millis).jittered).forkScoped
    _ <- doSomething.catchAll(_ => ZIO.unit).schedule(Schedule.spaced(200.millis).jittered).forkScoped
    _ <- FiberTree.run(2, 3, 3).forever.forkScoped
  } yield ()

  private lazy val fiberSupervisor = new FiberMonitor()

  override def run =
    (for {
      f <- ZIO.never.forkScoped
      _ <- program
      _ <- Server.serve[InsightPublisher with FiberEndpoint](InsightServer.routes)
      _ <- Console.printLine("Started Insight Sample application ...")
      _ <- f.join
    } yield ())
      .provideSome[Scope](
        ZLayer.succeed(ServerConfig.default.port(8080)),
        Server.live,
        ZLayer.succeed(MetricsConfig(5.seconds)),
        insight.metricsLayer,
        Runtime.enableRuntimeMetrics,
        Runtime.enableFiberRoots,
        DefaultJvmMetrics.live.unit,
        FiberEndpoint.live,
        ZLayer.succeed(
          fiberSupervisor,
        ),
        fiberSupervisor.layer,
      )
      .supervised(fiberSupervisor)
}
