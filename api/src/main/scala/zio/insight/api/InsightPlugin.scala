package zio.insight.api

import zio.Chunk

trait InsightPlugin {
  type Cmd
  type Msg

  def pluginId: Int
  def pluginTag: String

  def commandNames: Chunk[String] = Chunk.empty
  def messageNames: Chunk[String] = Chunk.empty

  def decodeCommand(cmd: Chunk[Byte]): Either[InsightError, Cmd]
  def encodeCommand(cmd: Cmd): Chunk[Byte]

  def decodeMessage(msg: Chunk[Byte]): Either[InsightError, Msg]
  def encodeMessage(msg: Msg): Chunk[Byte]
}
