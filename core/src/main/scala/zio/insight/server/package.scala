package zio.insight

import zio.ZLayer

package object server {

  def insightServerLayer(config: InsightServerConfig): ZLayer[Any, Throwable, Unit] =
    ZLayer.fromZIO(InsightServer.make(config))

}
