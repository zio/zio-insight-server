package zio.insight.redis

import zio._

sealed trait RedisParameter {
  def asString: String
}

object RedisParameter {
  final case class EX(value: Long) extends RedisParameter {
    override def asString = s"EX $value"
  }

  final case class PX(value: Long) extends RedisParameter {
    override def asString = s"PX $value"
  }

  final case object GET extends RedisParameter {
    override def asString = "GET"
  }

  final case object NX extends RedisParameter {
    override def asString = "NX"
  }

  final case object XX extends RedisParameter {
    override def asString = "XX"
  }

  final case class RedisString(value: String) extends RedisParameter {
    override def asString = value
  }

}

sealed trait RedisCommand {
  def name: String
  def parameter: Chunk[RedisParameter] = Chunk.empty
  def asString                         = {
    val params = parameter.map(_.asString).mkString(" ")
    s"$name $params\r\n"
  }
}

object RedisCommand {
  import RedisParameter._

  implicit class RedisCommandOps(self: RedisCommand) {
    def params(params: RedisParameter*): RedisCommand = self match {
      case cmd: SET => cmd.copy(options = cmd.options ++ Chunk.fromIterable(params))
      case _        => self
    }
  }

  final case object PING                                                                         extends RedisCommand { override val name = "ping" }
  final case class SET(key: String, value: String, options: Chunk[RedisParameter] = Chunk.empty) extends RedisCommand {
    override val name      = "SET"
    override val parameter = Chunk(RedisString(key), RedisString(value)) ++ options
  }
}
