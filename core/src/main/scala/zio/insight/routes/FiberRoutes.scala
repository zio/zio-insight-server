package zio.insight.routes

import zio._
import zio.http._
import zio.http.model._
import zio.insight.HttpUtils

object FiberRoutes {

  lazy val context: Path                             = !! / "insight" / "fibers"
  private def matchesPath(r: Path, p: Path): Boolean = HttpUtils.matchesPath(context)(r, p)

  private val fiberRoute = (ctxt: Path) =>
    Http.collectZIO[Request] {
      case Method.GET -> p if matchesPath(p, ~~ / "fibers") =>
        ZIO.succeed(Response.text("TODO"))
    }

  val routes = (p: Path) => fiberRoute(p)
}
