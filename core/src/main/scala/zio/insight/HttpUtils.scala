package zio.insight

import zhttp.http._

object HttpUtils {

  def noCors(r: Response): Response =
    r.updateHeaders(_.combine(Headers(("Access-Control-Allow-Origin", "*"))))

}
