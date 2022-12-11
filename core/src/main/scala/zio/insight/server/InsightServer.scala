package zio.insight.server

import zio._
import zio.json._
import zio.metrics.connectors.insight.{ClientMessage, InsightPublisher}
import zio.metrics.connectors.insight.ClientMessage.encAvailableMetrics

import zhttp.html._
import zhttp.http._
import zhttp.service.Server

object InsightServer {

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

  private[server] def run() =
    for {
      cfg <- ZIO.service[InsightServerConfig]
      svr <- server(cfg).start.forkDaemon
      _   <- Console.printLine(s"Started Insight Server with config $cfg")
      _   <- svr.join
    } yield ()

}
