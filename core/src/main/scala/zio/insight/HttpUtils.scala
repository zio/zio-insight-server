package zio.insight

import zio.http._
import zio.http.model._

object HttpUtils {

  def noCors(r: Response): Response =
    r.updateHeaders(_.combine(Headers(("Access-Control-Allow-Origin", "*"))))

}
