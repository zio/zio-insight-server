package zio.insight.fiber

import zio._

sealed trait FiberStatus

object FiberStatus {

  final case class Running(trace: Trace)                extends FiberStatus
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

final case class FiberInfo(
  id: FiberId.Runtime,
  parent: Option[FiberId.Runtime],
  status: FiberStatus)
