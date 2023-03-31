package zio.insight.routes

import zio._
import zio.http._
import zio.http.model._
import zio.insight.HttpUtils
import zio.insight.fiber._
import zio.json._

object FiberRoutes {
  lazy val context: Path = !! / "insight" / "fibers"

  private def matchesPath(r: Path, p: Path): Boolean = HttpUtils.matchesPath(context)(r, p)

  private val allFibersRoute: Path => Http[FiberEndpoint, Nothing, Request, Response] = ctxt =>
    Http.collectZIO[Request] {
      case Method.GET -> p if matchesPath(p, ~~ / "fibers") =>
        ZIO
          .serviceWithZIO[FiberEndpoint](_.fiberInfos())
          .map(infos => infos.toJson)
          .map(data => HttpUtils.noCors(Response.json(data)))
    }

  private val fiberTraceRoute: Path => Http[FiberEndpoint, Nothing, Request, Response] = ctxt =>
    Http.collectZIO[Request] {
      case Method.GET -> p if p.startsWith(context) =>
        val subPath = HttpUtils.relPath(context)(p)

        val id = HttpUtils.collect { case int(id) /: Path.empty =>
          id
        }(subPath)

        id match {
          case Some(id) =>
            ZIO
              .serviceWithZIO[FiberEndpoint](_.fiberInfo(id))
              .map(
                _.fold(Response.fromHttpError(HttpError.NotFound(s"Fiber with id $id not found")))(t =>
                  HttpUtils.noCors(Response.json(t.toJson)),
                ),
              )
          case None     => ZIO.succeed(Response.fromHttpError(HttpError.BadRequest("Invalid path")))
        }
    }

  val routes = (p: Path) => allFibersRoute(p) ++ fiberTraceRoute(p)
}
