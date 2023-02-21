package zio.insight.routes

import zio._
import zio.http._
import zio.http.model._

object FiberRoutes {

  private val fiberRoute =
    Http.collectZIO[Request] { case Method.GET -> !! / "fibers" =>
      ZIO.succeed(Response.text("TODO"))
    }

  val routes = fiberRoute
}
