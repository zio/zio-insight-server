package zio.insight.api

import zio.Chunk

trait InsightEncoder[-A] {
  def encode(a: A): Chunk[Byte]
}

trait InsightDecoder[+A] {
  def decode(bytes: Chunk[Byte]): Either[InsightError, A]
}
