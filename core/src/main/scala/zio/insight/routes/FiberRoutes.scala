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
          .serviceWithZIO[FiberEndpoint](_.fiberInfos(FiberTraceRequest.allFibers))
          .map(infos => infos.toJson)
          .map(data => HttpUtils.noCors(Response.json(data)))
    }

  private val fiberTracesRoute: Path => Http[FiberEndpoint, Nothing, Request, Response] = ctxt =>
    Http.collectZIO[Request] {
      case req @ Method.POST -> p if matchesPath(p, ~~ / "fibers") =>
        (for {
          body     <- req.body.asString
          request   = body.fromJson[FiberTraceRequest]
          response <- request match {
                        case Left(e)  =>
                          ZIO
                            .debug(s"Failed to parse the input: $e")
                            .as(
                              Response.text(e).setStatus(Status.BadRequest),
                            )
                        case Right(r) =>
                          ZIO
                            .serviceWithZIO[FiberEndpoint](_.fiberInfos(r))
                            .map(infos => infos.toJson)
                            .map(data => HttpUtils.noCors(Response.json(data)))
                      }
        } yield response).catchAll(t => ZIO.succeed(Response.text(t.getMessage).setStatus(Status.BadRequest)))
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
              .serviceWithZIO[FiberEndpoint](_.singleTrace(id))
              .map(
                _.fold(Response.fromHttpError(HttpError.NotFound(s"Fiber with id $id not found")))(t =>
                  HttpUtils.noCors(Response.json(t.toJson)),
                ),
              )
          case None     => ZIO.succeed(Response.fromHttpError(HttpError.BadRequest("Invalid path")))
        }
    }

  val routes = (p: Path) => allFibersRoute(p) ++ fiberTraceRoute(p) ++ fiberTracesRoute(p)
}
