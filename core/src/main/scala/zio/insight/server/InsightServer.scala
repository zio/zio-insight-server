package zio.insight.server

import zio._
import zio.insight.server.InsightServer.{bindPort, static}
import zio.json._
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.insight.{ClientMessage, InsightPublisher}
import zio.metrics.connectors.insight.ClientMessage.encAvailableMetrics
import zio.metrics.jvm.DefaultJvmMetrics

import zhttp.html._
import zhttp.http._
import zhttp.service.{EventLoopGroup, Server}
import zhttp.service.server.ServerChannelFactory

object InsightServer {

  private val bindPort = 8080
  private val nThreads = 5

  private val metricsConfig = ZLayer.succeed(MetricsConfig(5.seconds))

  private lazy val indexPage =
    """<html>
      |<title>Simple Server</title>
      |<body>
      |<p><a href="/insight/keys">Insight Metrics: Get all keys</a></p>
      |</body
      |</html>""".stripMargin

  private lazy val static =
    Http.collect[Request] { case Method.GET -> !! => Response.html(Html.fromString(indexPage)) }

  private lazy val insightAllKeysRouter =
    Http.collectZIO[Request] { case Method.GET -> !! / "insight" / "keys" =>
      ZIO.serviceWithZIO[InsightPublisher](_.getAllKeys.map(_.toJson).map(data => noCors(Response.json(data))))
    }

  // POST: /insight/metrics body Seq[MetricKey] => Seq[MetricsNotification]
  // TODO: Should we add an additional module with a layer implementation for zio-http?
  // should be added (at some point) to zio-http ...
  private lazy val insightGetMetricsRouter =
    Http.collectZIO[Request] { case req @ Method.POST -> !! / "insight" / "metrics" =>
      for {
        request  <- req.body.asString.map(_.fromJson[ClientMessage.MetricsSelection])
        response <- request match {
                      case Left(e)  =>
                        ZIO
                          .debug(s"Failed to parse the input: $e")
                          .as(
                            Response.text(e).setStatus(Status.BadRequest),
                          )
                      case Right(r) =>
                        ZIO
                          .serviceWithZIO[InsightPublisher](_.getMetrics(r.selection))
                          .map(_.toJson)
                          .map(data => noCors(Response.json(data)))
                    }
      } yield response
    }

  private def noCors(r: Response): Response =
    r.updateHeaders(_.combine(Headers(("Access-Control-Allow-Origin", "*"))))

  private def server(config: InsightServerConfig): Server[InsightPublisher, Throwable] =
    Server.port(config.port) ++ Server.app(static ++ insightAllKeysRouter ++ insightGetMetricsRouter)

  private lazy val runHttp =
    ZIO.service[InsightServerConfig].flatMap(config => (server(config).start *> ZIO.never).forkDaemon)

  def make(config: InsightServerConfig): ZIO[Any, Throwable, Unit] = (for {

    f <- runHttp
    _ <- f.join
  } yield ())
    .provide(
      ServerChannelFactory.auto,
      EventLoopGroup.auto(nThreads),

      // Server config
      ZLayer.succeed(config),

      // Metrics config
      ZLayer.succeed(MetricsConfig(config.interval)),

      // The insight reporting layer
      zio.metrics.connectors.insight.metricsLayer,

      // Enable the ZIO internal metrics and the default JVM metricsConfig
      // Do NOT forget the .unit for the JVM metrics layer
      Runtime.enableRuntimeMetrics,
      Runtime.enableFiberRoots,
      DefaultJvmMetrics.live.unit,
    )
}
