package zio.insight.redis

import zio._

trait RedisExecutor {
  def execute(cmd: RedisCommand): ZIO[Scope, Throwable, String]
}

object RedisExecutor {

  private def make(conn: RedisConnection) =
    new RedisExecutor {
      override def execute(cmd: RedisCommand): ZIO[Scope, Throwable, String] =
        conn.execute(cmd.asString + "\r\n")
    }

  lazy val live = ZLayer.fromZIO(
    ZIO.logInfo(s"Creating Redis Executor ...") *>
      ZIO.service[RedisConnection].map(conn => make(conn)),
  )
}
