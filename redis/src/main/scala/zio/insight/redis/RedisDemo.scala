package zio.insight.redis

import zio._

import RedisCommand._
import RedisParameter._

object RedisDemo extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs with Scope, Any, ExitCode] = {
    val program: ZIO[Scope with RedisExecutor, Throwable, String] = for {
      _      <- ZIO.logInfo("Starting Redis Demo")
      result <-
        ZIO.serviceWithZIO[RedisExecutor](
          _.execute(
            RedisCommand.SET("name", "Andreas").params(EX(10), GET),
          ),
        )
    } yield result

    program
      .provide(
        Scope.default,
        RedisConnection.live,
        RedisExecutor.live,
      )
  }.exitCode
}
