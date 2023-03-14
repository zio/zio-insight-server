package zio.insight.fiber

import zio._
import zio.json._

final case class FiberInfo private (
  id: FiberId,
  parent: Option[FiberId],
  children: Chunk[FiberId])

object FiberInfo {

  implicit val locationEnc =
    JsonEncoder.tuple3[String, String, Int].contramap[Trace] { l: Trace =>
      Trace.unapply(l).getOrElse(("", "", 0))
    }

  implicit val locationDec =
    JsonDecoder.tuple3[String, String, Int].map {
      case ("", "", 0)       => Trace.empty
      case (loc, file, line) => Trace(loc, file, line)
    }

  implicit val fiberIdCodec: JsonCodec[FiberId] = DeriveJsonCodec.gen[FiberId]

  implicit val fiberIfoCodec = DeriveJsonCodec.gen[FiberInfo]

  def fromFiber(fiber: Fiber.Runtime[_, _]): FiberInfo =
    FiberInfo(
      fiber.id,
      None,
      Chunk.empty,
    )
}
