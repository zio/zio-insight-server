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
  def fiberInfos(req: FiberTraceRequest): UIO[Chunk[FiberInfo]]
  def singleTrace(id: Int): UIO[Option[FiberInfo]]
}

abstract private[insight] class FiberEndpointImpl(monitor: FiberMonitor) extends FiberEndpoint {
  def fiberInfos(req: FiberTraceRequest): UIO[Chunk[FiberInfo]] =
    ZIO.collectAll(monitor.filteredFibers(req))

  def singleTrace(id: Int): UIO[Option[FiberInfo]] =
    monitor.fiberTrace(id)
}

object FiberEndpoint {

  lazy val live: ZLayer[FiberMonitor, Nothing, FiberEndpoint] =
    ZLayer.fromZIO(
      ZIO.service[FiberMonitor].map(new FiberEndpointImpl(_) {}),
    )

  def fiberInfos(req: FiberTraceRequest) = ZIO.serviceWithZIO[FiberEndpoint](_.fiberInfos(req))
  def singleTrace(id: Int)               = ZIO.serviceWithZIO[FiberEndpoint](_.singleTrace(id))

}
