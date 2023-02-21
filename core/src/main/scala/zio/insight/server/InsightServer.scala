package zio.insight.server

import zio.http._
import zio.http.html._
import zio.http.model._
import zio.insight.routes._

object InsightServer {

  private lazy val indexPage =
    """<html>
      |<title>Simple Server</title>
      |<body>
      |<p><a href="/insight/metrics/keys">Insight Metrics: Get all keys</a></p>
      |</body
      |</html>""".stripMargin

  lazy val routes =
    // static routes
    Http.collect[Request] { case Method.GET -> !! => Response.html(Html.fromString(indexPage)) } ++
      // Metric routes
      Http.collectRoute[Request] { case _ -> !! / "insight" / "metrics" => MetricRoutes.routes } ++
      // Fiber routes
      Http.collectRoute[Request] { case _ -> !! / "insight" / "fibers" => FiberRoutes.routes }

}
