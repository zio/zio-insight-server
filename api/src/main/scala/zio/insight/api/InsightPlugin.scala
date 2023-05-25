package zio.insight.api

import zio._

trait InsightPlugin {

  import InsightPlugin.MsgTag

  type Message

  def pluginId: Int

  def msgTags = Chunk.empty[MsgTag]
  def msgTag(msg: Message): MsgTag

  def encoder: InsightEncoder[Message]
  def decoder: InsightDecoder[Message]

  def send(msg: Message): ZIO[Any, InsightError, Unit]
  def handle(msg: Message): ZIO[Any, InsightError, Unit]
}

object InsightPlugin {
  type MsgTag = String
}
