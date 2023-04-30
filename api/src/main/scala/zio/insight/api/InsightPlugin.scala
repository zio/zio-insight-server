package zio.insight.api

import zio._

trait InsightPlugin {
  type Message

  def pluginId: Int

  def messageNames = Chunk.empty[String]
  def msgTag(msg: Message): String

  def encoder: InsightEncoder[Message]
  def decoder: InsightDecoder[Message]

  def send(msg: Message): ZIO[Any, InsightError, Unit]
  def handle(msg: Message): ZIO[Any, InsightError, Unit]
}
