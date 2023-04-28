import zio._

sealed trait FiberStatus

object FiberStatus {

  final case class Running(currentLocation: Trace)      extends FiberStatus
  final case class Suspended(
    blockingOn: Set[FiberId.Runtime],
    currentLocation: Trace)
      extends FiberStatus
  final case class Succeeded(endedAt: Long)             extends FiberStatus
  final case class Errored(endedAt: Long, hint: String) extends FiberStatus

  def fromZIO(s: Fiber.Status): FiberStatus = s match {
    case Fiber.Status.Running(_, trace)               => Running(trace)
    case Fiber.Status.Suspended(_, trace, blockingOn) => Suspended(blockingOn.toSet, trace)
    case Fiber.Status.Done                            => Succeeded(java.lang.System.currentTimeMillis())
  }
}

final case class FiberTraceRequest(
  // If set, only include fibers in the response that are descendants of the fiber with the given id
  root: Option[Int],

  // If set to true, only include active fibers in the response, otherwise also include
  // terminated fibers and their results.
  activeOnly: Boolean,

  // The ids of the fibers with a trace to be included in the response
  traced: Set[Int])

object FiberTraceRequest {
  val allFibers = FiberTraceRequest(None, false, Set.empty)
}

final case class FiberInfo(
  id: FiberId.Runtime,
  parent: Option[FiberId.Runtime],
  status: FiberStatus,
  stacktrace: Chunk[Trace])
