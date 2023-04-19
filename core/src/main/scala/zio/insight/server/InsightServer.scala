package zio.insight.server

import zio.http._
import zio.http.html._
import zio.http.model._
import zio.insight.fiber.FiberEndpoint
import zio.insight.routes._
import zio.metrics.connectors.insight.InsightPublisher

object InsightServer {

  private lazy val indexPage =
    """<html>
      |<title>Simple Server</title>
      |<body>
      |<p><a href="/insight/metrics/keys">Insight Metrics: Get all keys</a></p>
      |</body
      |</html>""".stripMargin

  lazy val routes: Http[InsightPublisher with FiberEndpoint, Nothing, Request, Response] =
    // static routes
    Http.collect[Request] { case Method.GET -> !! =>
      Response.html(Html.fromString(indexPage))
    } ++
      // Metric routes
      Http.collectRoute[Request] {
        case _ -> p if p.startsWith(MetricRoutes.context) =>
          MetricRoutes.routes(p)
      } ++
      // Fiber routes
      Http.collectRoute[Request] {
        case _ -> p if p.startsWith(FiberRoutes.context) =>
          FiberRoutes.routes(p)
      }

}
