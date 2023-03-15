package zio.insight.fiber

import zio._

/**
 * The FiberEndpoint provides information about the fibers running in the system.
 * It provides visibility for the fibers that are currently selected within th client.
 * Also, for each fiber we keep the chain of parents for easier Navigation.
 * The mental model for the FiberEndpoint is a tree with collapsible nodes with only the
 * root fiber expanded by default.
 *
 * For each fiber we can also provide a stacktrace on demand.
 */
trait FiberEndpoint {

  def fiberInfos(): UIO[Chunk[FiberInfo]]
  def fiberTrace(id: FiberId): UIO[Option[String]]

}

abstract private[insight] class FiberEndpointImpl(monitor: FiberMonitor) extends FiberEndpoint {

  def fiberInfos(): UIO[Chunk[FiberInfo]] =
    ZIO.collectAllPar(monitor.allFibers())

  def fiberTrace(id: FiberId): UIO[Option[String]] = ZIO.none
}

object FiberEndpoint {

  lazy val live: ZLayer[FiberMonitor, Nothing, FiberEndpoint] =
    ZLayer.fromZIO(
      ZIO.service[FiberMonitor].map(new FiberEndpointImpl(_) {}),
    )

  def fiberInfos() = ZIO.serviceWithZIO[FiberEndpoint](_.fiberInfos())

  def fiberTrace(id: FiberId) =
    ZIO.serviceWithZIO[FiberEndpoint](_.fiberTrace(id))

}
