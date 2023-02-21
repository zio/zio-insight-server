package zio.insight.routes

import zio._

import zhttp.http._

object FiberRoutes {

  private val fiberRoute =
    Http.collectZIO[Request] { case Method.GET -> !! / "fibers" =>
      ZIO.succeed(Response.text("TODO"))
    }

  val routes = fiberRoute
}
