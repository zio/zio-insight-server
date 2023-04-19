package zio.insight

import zio.http._
import zio.http.model._

object HttpUtils {

  def relPath(base: Path)(p: Path): Path                          = if (p.startsWith(base)) p.drop(base.segments.size) else p
  def matchesPath(base: Path)(r: Path, p: Path): Boolean          = relPath(base)(r).equals(p)
  def collect[A](pf: PartialFunction[Path, A]): Path => Option[A] = path => pf.lift(path)

  def noCors(r: Response): Response =
    r.updateHeaders(_.combine(Headers(("Access-Control-Allow-Origin", "*"))))

}
