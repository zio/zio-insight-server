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

  private val entries = new ConcurrentHashMap[Int, (FiberMapEntry, Option[FiberStatus])]()

  private val getInfo: (FiberMapEntry, Option[FiberStatus]) => ZIO[Any, Nothing, FiberInfo] =
    (e: FiberMapEntry, s: Option[FiberStatus]) =>
      (s match {
        case Some(s) => ZIO.succeed(s)
        case None    => e.fiber.status.map(FiberStatus.fromZIO)
      }).map { s =>
        FiberInfo(
          e.fiber.id,
          e.parent.map(_.id),
          s,
          Chunk.empty,
        )
      }

  def allFibers(): Chunk[UIO[FiberInfo]] = {
    val builder = ChunkBuilder.make[UIO[FiberInfo]]()

    entries
      .values()
      .forEach { case (entry: FiberMapEntry, status: Option[FiberStatus]) =>
        builder += getInfo(entry, status)
      }

    builder.result()
  }

  def fiberTrace(id: Int): UIO[Option[FiberInfo]] = {
    val mbEntry = Option(entries.get(id))
    mbEntry match {
      case Some((entry, state)) =>
        entry.fiber.dump
          .map(_.trace.stackTrace)
          .zipPar(getInfo(entry, state))
          .map { case (trace, info) => Some(info.copy(stacktrace = trace)) }
      case None                 => ZIO.none
    }
  }

  lazy val layer: ZLayer[Any, Nothing, Unit] =
    ZLayer.fromZIO(
      cleanUp
        .schedule(Schedule.spaced(5000.millis))
        .forkDaemon
        .unit,
    )

  private lazy val cleanUp: ZIO[Any, Nothing, Unit] = {

    def checkEntry(
      id: Int,
      ended: Long,
      now: Long,
      delay: Long,
    ): Unit = {
      if (now - ended > delay) entries.remove(id)
      ()
    }

    def doClean(now: Long): Unit =
      entries.forEach { (id: Int, entry: (FiberMapEntry, Option[FiberStatus])) =>
        entry._2 match {
          case Some(FiberStatus.Succeeded(t))  => checkEntry(id, t, now, 5.seconds.toMillis)
          case Some(FiberStatus.Errored(t, _)) => checkEntry(id, t, now, 10.seconds.toMillis)
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
    entries.put(fiber.id.id, (FiberMapEntry(parent, fiber), None))
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
          fiber.id.id,
          (entries.get(fiber.id.id)._1, Some(FiberStatus.Succeeded(now))),
        )
      case Exit.Failure(e) =>
        val hint = e match {
          case Cause.Die(e, _) => e.getMessage
          case _               => e.toString
        }
        entries.put(
          fiber.id.id,
          (entries.get(fiber.id.id)._1, Some(FiberStatus.Errored(now, hint))),
        )
    }
    ()
  }
}
