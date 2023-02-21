package zio.insight.routes

import zio._
import zio.insight.HttpUtils
import zio.json._
import zio.metrics.connectors.insight.ClientMessage
import zio.metrics.connectors.insight.InsightPublisher

import zhttp.http._

object MetricRoutes {

  private lazy val static = Http.collect[Request] { case Method.GET -> !! / "metrics" =>
    Response.text("TODO")
  }

  private lazy val allKeysRoute =
    Http.collectZIO[Request] { case Method.GET -> !! / "keys" =>
      ZIO.serviceWithZIO[InsightPublisher](
        _.getAllKeys.map(_.toJson).map(data => HttpUtils.noCors(Response.json(data))),
      )
    }

  // POST: /insight/metrics body Seq[MetricKey] => Seq[MetricsNotification]
  // TODO: Should we add an additional module with a layer implementation for zio-http?
  // should be added (at some point) to zio-http ...
  private lazy val getMetricsRoute =
    Http.collectZIO[Request] { case req @ Method.POST -> !! / "metrics" =>
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
                          .map(data => HttpUtils.noCors(Response.json(data)))
                    }
      } yield response
    }

  val routes = static ++ allKeysRoute ++ getMetricsRoute
}
