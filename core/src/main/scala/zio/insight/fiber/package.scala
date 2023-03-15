package zio.insight

import zio._
import zio.json._

package object fiber {
  implicit val locationEnc =
    JsonEncoder.tuple3[String, String, Int].contramap[Trace] { l: Trace =>
      Trace.unapply(l).getOrElse(("", "", 0))
    }

  implicit val locationDec =
    JsonDecoder.tuple3[String, String, Int].map {
      case ("", "", 0)       => Trace.empty
      case (loc, file, line) => Trace(loc, file, line)
    }

  implicit val fiberIdCodec: JsonCodec[FiberId.Runtime] = DeriveJsonCodec.gen[FiberId.Runtime]

  implicit val fiberStatusCodec: JsonCodec[FiberStatus] = DeriveJsonCodec.gen[FiberStatus]

  implicit val fiberIfoCodec = DeriveJsonCodec.gen[FiberInfo]

}
