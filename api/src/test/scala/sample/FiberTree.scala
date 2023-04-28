import zio._

object FiberTree {

  def run(minChildren: Int, maxChildren: Int, maxDepth: Int): ZIO[Scope, Nothing, Int] =
    for {
      // How many children do we want to spawn
      cnt <- ZIO.randomWith(_.nextIntBetween(minChildren, maxChildren))
      // spawn all of them in parallel - should spawn a fiber for each child
      res <- ZIO.foreachPar(0.to(cnt)) { _ =>
               if (maxDepth > 0) run(minChildren, maxChildren, maxDepth - 1)
               else
                 for {
                   d <- ZIO.randomWith(_.nextIntBetween(30, 60))
                   f <-
                     ZIO
                       .randomWith(_.nextIntBetween(0, 10))
                       .schedule(Schedule.spaced(200.millis))
                       .forever
                       .forkScoped
                   _ <- f.interrupt.delay(d.seconds)
                 } yield cnt
             }
    } yield res.sum
}
