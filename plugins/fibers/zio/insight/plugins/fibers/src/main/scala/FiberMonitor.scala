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

  private def getInfo(
    entry: FiberMapEntry,
    status: Option[FiberStatus],
    req: FiberTraceRequest,
  ): ZIO[Any, Nothing, FiberInfo] = for {
    state <- status match {
               case Some(s) => ZIO.succeed(s)
               case None    => entry.fiber.status.map(FiberStatus.fromZIO)
             }
    trace <- req.traced.contains(entry.fiber.id.id) match {
               case true  => entry.fiber.dump.map(_.trace.stackTrace)
               case false => ZIO.succeed(Chunk.empty)
             }
  } yield FiberInfo(
    entry.fiber.id,
    entry.parent.map(_.id),
    state,
    trace,
  )

  def filteredFibers(req: FiberTraceRequest): Chunk[UIO[FiberInfo]] = {
    val builder = ChunkBuilder.make[UIO[FiberInfo]]()

    entries
      .values()
      .forEach { case (entry: FiberMapEntry, status: Option[FiberStatus]) =>
        val includeActive   = !req.activeOnly || status.isEmpty
        // only trace the dependencies to the root if the entry is active and a root is set
        // An unset root means we want all fibers
        val includeWithRoot = includeActive && req.root.fold(true)(isDescendantOf(entry, _))
        // If the request has a root, only include fibers that are descendants of the root
        if (includeWithRoot)
          builder += getInfo(entry, status, req)
      }

    builder.result()
  }

  def fiberTrace(id: Int): UIO[Option[FiberInfo]] = {
    val mbEntry = Option(entries.get(id))
    mbEntry match {
      case Some((entry, state)) =>
        entry.fiber.dump
          .map(_.trace.stackTrace)
          .zipPar(getInfo(entry, state, FiberTraceRequest.allFibers))
          .map { case (trace, info) => Some(info.copy(stacktrace = trace)) }
      case None                 => ZIO.none
    }
  }

  private def isDescendantOf(
    descendant: FiberMapEntry,
    root: Int,
  ): Boolean =
    if (descendant.fiber.id.id == root) true
    else
      descendant.parent match {
        case None    => false
        case Some(p) =>
          val entry = entries.get(p.id.id)
          entry != null && isDescendantOf(entry._1, root)
      }

  lazy val layer: ZLayer[Any, Nothing, Unit] =
    ZLayer.fromZIO(
      maintain
        .schedule(Schedule.spaced(5000.millis))
        .forkDaemon
        .unit,
    )

  private lazy val maintain: ZIO[Any, Nothing, Unit] = {

    def gcEntry(
      id: Int,
      ended: Long,
      now: Long,
      delay: Long,
    ): Unit =
      if (now - ended > delay) {
        entries.remove(id)
        ()
      }

    def doClean(now: Long): Int = {
      entries.forEach { (id: Int, entry: (FiberMapEntry, Option[FiberStatus])) =>
        entry._2 match {
          case Some(FiberStatus.Succeeded(t))  => gcEntry(id, t, now, 5.seconds.toMillis)
          case Some(FiberStatus.Errored(t, _)) => gcEntry(id, t, now, 10.seconds.toMillis)
          case Some(_)                         => ()
          case None                            => ()
        }
      }
      entries.size()
    }

    for {
      now      <- ZIO.clockWith(_.currentTime(TimeUnit.MILLISECONDS))
      remaining = doClean(now)
    } yield ()
  }

  def value(implicit trace: zio.Trace): UIO[Unit] = ZIO.unit

  private def checkEntry(parent: Option[Fiber.Runtime[_, _]], fiber: Fiber.Runtime[_, _]): Unit = {
    entries.putIfAbsent(fiber.id.id, (FiberMapEntry(parent, fiber), None))
    parent.foreach(p => entries.putIfAbsent(p.id.id, (FiberMapEntry(None, p), None)))
  }

  override def onStart[R, E, A](
    environment: ZEnvironment[R],
    effect: ZIO[R, E, A],
    parent: Option[Fiber.Runtime[Any, Any]],
    fiber: Fiber.Runtime[E, A],
  )(implicit unsafe: Unsafe,
  ): Unit = checkEntry(parent, fiber)

  override def onSuspend[E, A](fiber: Fiber.Runtime[E, A])(implicit unsafe: Unsafe): Unit                      = checkEntry(None, fiber)
  override def onEffect[E, A](fiber: Fiber.Runtime[E, A], effect: ZIO[_, _, _])(implicit unsafe: Unsafe): Unit =
    checkEntry(None, fiber)
  override def onResume[E, A](fiber: Fiber.Runtime[E, A])(implicit unsafe: Unsafe): Unit                       = checkEntry(None, fiber)

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
