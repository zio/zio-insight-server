package zio.insight.redis

import java.io.EOFException
import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.Channel
import java.nio.channels.CompletionHandler

import zio._
import zio.stream.ZStream

trait RedisConnection {
  def execute(cmd: String): ZIO[Scope, Throwable, String]
}

object RedisConnection {
  private def make(): ZIO[Scope, Throwable, RedisConnection] = for {
    rb <- ZIO.attempt(ByteBuffer.allocate(1024))
    wb <- ZIO.attempt(ByteBuffer.allocate(1024))
    ch <- openChannel(new InetSocketAddress("localhost", 6379)).tapError(e =>
            ZIO.logError(s"Failed to connect to REDIS : $e"),
          )
  } yield new RedisConnectionImpl(rb, wb, ch) {}

  private def openChannel(address: InetSocketAddress) =
    ZIO
      .fromAutoCloseable {
        for {
          channel <- ZIO.attempt {
                       val channel = AsynchronousSocketChannel.open()
                       channel.setOption(StandardSocketOptions.SO_KEEPALIVE, Boolean.box(true))
                       channel.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.box(true))
                       channel
                     }
          _       <- closeWith[Void](channel)(channel.connect(address, null, _))
          _       <- ZIO.logInfo(s"Connected to the redis server with address $address.")
        } yield channel
      }

  private def completionHandler[A](k: IO[IOException, A] => Unit): CompletionHandler[A, Any] =
    new CompletionHandler[A, Any] {
      def completed(result: A, u: Any): Unit = k(ZIO.succeed(result))

      def failed(t: Throwable, u: Any): Unit =
        t match {
          case e: IOException => k(ZIO.fail(e))
          case _              => k(ZIO.die(t))
        }
    }

  private def closeWith[A](channel: Channel)(op: CompletionHandler[A, Any] => Any): IO[IOException, A] =
    ZIO.asyncInterrupt { k =>
      op(completionHandler(k))
      Left(ZIO.attempt(channel.close()).ignore)
    }

  sealed abstract class RedisConnectionImpl(
    readBuffer: ByteBuffer,
    writeBuffer: ByteBuffer,
    channel: AsynchronousSocketChannel)
      extends RedisConnection {

    private def send(chunk: Chunk[Byte]): ZIO[Any, Throwable, Option[Unit]] =
      ZIO.when(chunk.nonEmpty) {
        ZIO.suspendSucceed {
          writeBuffer.clear()
          val (c, remainder) = chunk.splitAt(writeBuffer.capacity())
          println(s"Sending ${c.size} bytes")
          writeBuffer.put(c.toArray)
          writeBuffer.flip()

          closeWith[Integer](channel)(channel.write(writeBuffer, null, _))
            .repeatWhile(_ => writeBuffer.hasRemaining)
            .zipRight(send(remainder))
            .map(_.getOrElse(()))
        }
      }

    private def receive(): ZStream[Any, IOException, Byte] = ZStream.repeatZIOChunkOption {
      val receive =
        for {
          _     <- ZIO.succeed(readBuffer.clear())
          _     <- closeWith[Integer](channel)(channel.read(readBuffer, null, _)).filterOrFail(_ >= 0)(new EOFException())
          chunk <- ZIO.succeed {
                     readBuffer.flip()
                     val count = readBuffer.remaining()
                     val array = Array.ofDim[Byte](count)
                     readBuffer.get(array)
                     Chunk.fromArray(array)
                   }
        } yield chunk

      receive.mapError {
        case _: EOFException => None
        case e: IOException  => Some(e)
      }
    }

    def execute(cmd: String): zio.ZIO[Scope, Throwable, String] =
      for {
        _   <- ZIO.logInfo(s"Executing command : $cmd")
        f   <- receive().takeUntil(_ == '\n').runCollect.map(_.toArray).map(new String(_)).forkScoped
        _   <- ZIO.logInfo(s"Started Receiver ... ")
        _   <- send(Chunk.fromArray(cmd.getBytes))
        res <- f.join
        _   <- ZIO.logInfo(s"Command Result : $res")
      } yield res
  }

  val live = ZLayer.fromZIO(
    ZIO.logInfo("Creating Redis Connection ...") *>
      make(),
  )
}
