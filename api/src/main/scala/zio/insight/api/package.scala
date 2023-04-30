package zio.insight

import zio.insight.api.protocol.InsightMessage.InsightMessage

package object api {

  implicit class InsightMessageOps(self: InsightMessage) {

    private def byteToHex(b: Byte): String = {
      val digit: Int => Char = Character.forDigit(_, 16)
      new String(Array(digit((b >> 4) & 0xf), digit(b & 0xf)))
    }

    private def byteToHex(arr: Array[Byte]): String = arr.map(byteToHex).mkString(" ")

    def dump =
      s"InsightMessage(${self.appId}, ${self.plugin}, ${self.msgTag}, ${byteToHex(self.payload.toByteArray())})"
  }

}
