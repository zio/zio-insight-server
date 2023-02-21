package zio.insight.fiber

import zio._

final case class FiberInfo private (
  id: String)

object FiberInfo {

  def fromFiber(fiber: Fiber.Runtime[_, _]): FiberInfo =
    FiberInfo(
      s"${fiber.id}",
    )
}
