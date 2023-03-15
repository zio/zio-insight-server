package zio.insight.fiber

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import zio._
import zio.UIO

object FiberMonitor {
  final case class FiberMapEntry(
    parent: Option[Fiber.Runtime[_, _]],
    fiber: Fiber.Runtime[_, _])
}

class FiberMonitor() extends Supervisor[Unit] {

  import FiberMonitor._

  private val entries = new ConcurrentHashMap[FiberId.Runtime, (FiberMapEntry, Option[FiberStatus])]()

  def allFibers(): Chunk[UIO[FiberInfo]] = {
    val builder = ChunkBuilder.make[UIO[FiberInfo]]()

    entries
      .values()
      .forEach { case (entry: FiberMapEntry, status: Option[FiberStatus]) =>
        val state = status match {
          case Some(s) => ZIO.succeed(s)
          case None    => entry.fiber.status.map(FiberStatus.fromZIO)
        }

        val info = state.map { s =>
          FiberInfo(
            entry.fiber.id,
            entry.parent.map(_.id),
            s,
          )
        }

        builder += info
      }

    builder.result()
  }

  lazy val layer: ZLayer[Any, Nothing, Unit] =
    ZLayer.fromZIO(
      cleanUp
        .schedule(Schedule.spaced(5000.millis))
        .forkDaemon
        .unit,
    )

  private lazy val cleanUp: ZIO[Any, Nothing, Unit] = {

    def checkEntry(id: FiberId.Runtime, ended: Long, now: Long): Unit = {
      if (now - ended > 5000) entries.remove(id)
      ()
    }

    def doClean(now: Long): Unit =
      entries.forEach { (id: FiberId.Runtime, entry: (FiberMapEntry, Option[FiberStatus])) =>
        entry._2 match {
          case Some(FiberStatus.Succeeded(t))  => checkEntry(id, t, now)
          case Some(FiberStatus.Errored(t, _)) => checkEntry(id, t, now)
          case Some(_)                         => ()
          case None                            => ()
        }
      }

    for {
      now <- ZIO.clockWith(_.currentTime(TimeUnit.MILLISECONDS))
      _    = doClean(now)
    } yield ()
  }

  def value(implicit trace: zio.Trace): UIO[Unit] = ZIO.unit

  override def onStart[R, E, A](
    environment: ZEnvironment[R],
    effect: ZIO[R, E, A],
    parent: Option[Fiber.Runtime[Any, Any]],
    fiber: Fiber.Runtime[E, A],
  )(implicit unsafe: Unsafe,
  ): Unit = {
    entries.put(fiber.id, (FiberMapEntry(parent, fiber), None))
    ()
  }

  override def onSuspend[E, A](fiber: Fiber.Runtime[E, A])(implicit unsafe: Unsafe): Unit = {}

  override def onEffect[E, A](fiber: Fiber.Runtime[E, A], effect: ZIO[_, _, _])(implicit unsafe: Unsafe): Unit = {}

  override def onResume[E, A](fiber: Fiber.Runtime[E, A])(implicit unsafe: Unsafe): Unit = {}

  override def onEnd[R, E, A](value: Exit[E, A], fiber: Fiber.Runtime[E, A])(implicit unsafe: Unsafe): Unit = {
    val now = java.lang.System.currentTimeMillis()
    value match {
      case Exit.Success(_) =>
        entries.put(
          fiber.id,
          (entries.get(fiber.id)._1, Some(FiberStatus.Succeeded(now))),
        )
      case Exit.Failure(e) =>
        val hint = e match {
          case Cause.Die(e, _) => e.getMessage
          case _               => e.toString
        }
        entries.put(
          fiber.id,
          (entries.get(fiber.id)._1, Some(FiberStatus.Errored(now, hint))),
        )
    }
    ()
  }
}
