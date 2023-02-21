package zio.insight.server

import zio._
import zio.insight.routes._

import zhttp.html._
import zhttp.http._
import zhttp.service.Server

object InsightServer {

  private lazy val indexPage =
    """<html>
      |<title>Simple Server</title>
      |<body>
      |<p><a href="/insight/metrics/keys">Insight Metrics: Get all keys</a></p>
      |</body
      |</html>""".stripMargin

  private lazy val routes =
    // static routes
    Http.collect[Request] { case Method.GET -> !! => Response.html(Html.fromString(indexPage)) } ++
      // Metric routes
      Http.collectHttp[Request] { case _ -> !! / "insight" / "metrics" => MetricRoutes.routes } ++
      // Fiber routes
      Http.collectHttp[Request] { case _ -> !! / "insight" / "fibers" => FiberRoutes.routes }

  private[server] def run() =
    for {
      cfg <- ZIO.service[InsightServerConfig]
      svr <- Server
               .start(cfg.port, routes)
               .forkDaemon
      _   <- Console.printLine(s"Started Insight Server at port (${cfg.port})...")
      _   <- svr.join
    } yield ()

}
