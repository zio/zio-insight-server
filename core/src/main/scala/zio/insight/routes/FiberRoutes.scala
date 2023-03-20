package zio.insight.routes

import zio._
import zio.http._
import zio.http.model._
import zio.insight.HttpUtils
import zio.insight.fiber.FiberEndpoint
import zio.json._

object FiberRoutes {

  lazy val context: Path                             = !! / "insight" / "fibers"
  private def matchesPath(r: Path, p: Path): Boolean = HttpUtils.matchesPath(context)(r, p)

  private val fiberRoute: Path => Http[FiberEndpoint, Nothing, Request, Response] = ctxt =>
    Http.collectZIO[Request] {
      case Method.GET -> p if matchesPath(p, ~~ / "fibers") =>
        ZIO
          .serviceWithZIO[FiberEndpoint](_.fiberInfos())
          .map(infos => infos.toJson)
          .map(data => HttpUtils.noCors(Response.json(data)))
    }

  val routes = (p: Path) => fiberRoute(p)
}
