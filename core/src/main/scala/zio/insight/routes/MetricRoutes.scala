package zio.insight.routes

import zio._
import zio.http._
import zio.http.model._
import zio.insight.HttpUtils
import zio.json._
import zio.metrics.connectors.insight.ClientMessage
import zio.metrics.connectors.insight.InsightPublisher

object MetricRoutes {

  lazy val context                                   = !! / "insight" / "metrics"
  private def matchesPath(r: Path, p: Path): Boolean = HttpUtils.matchesPath(context)(r, p)

  private lazy val allKeysRoute: Path => Http[InsightPublisher, Nothing, Request, Response] = ctxt =>
    Http.collectZIO[Request] {
      case Method.GET -> p if matchesPath(p, ~~ / "keys") =>
        ZIO.serviceWithZIO[InsightPublisher](
          _.getAllKeys.map(_.toJson).map(data => HttpUtils.noCors(Response.json(data))),
        )
    }

  // POST: /insight/metrics body Seq[MetricKey] => Seq[MetricsNotification]
  // TODO: Should we add an additional module with a layer implementation for zio-http?
  // should be added (at some point) to zio-http ...
  private lazy val getMetricsRoute: Path => Http[InsightPublisher, Nothing, Request, Response] = ctxt =>
    Http.collectZIO[Request] {
      case req @ Method.POST -> p if matchesPath(p, ~~ / "metrics") =>
        (for {
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
                            .map(data => HttpUtils.noCors(Response.json(data)))
                      }
        } yield response).orDie // TODO: proper error handling
    }

  val routes: Path => Http[InsightPublisher, Nothing, Request, Response] = ctxt =>
    allKeysRoute(ctxt) ++ getMetricsRoute(ctxt)
}
