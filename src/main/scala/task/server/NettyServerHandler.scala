
package task.server

import java.io.Closeable
import java.nio.charset.StandardCharsets.US_ASCII

import com.typesafe.scalalogging.Logger
import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.apache.commons.lang3.math.NumberUtils
import task.Buffers
import task.Buffers.ByteBufOps
import task.Constants.AllowedChars
import task.store.Queue

import scala.language.postfixOps

class NettyServerHandler(
  private val connection: SocketChannel,
  private val q: Queue,
  private val server: Closeable
) extends SimpleChannelInboundHandler[ByteBuf] {

  import NettyServerHandler._

  require(connection != null)
  require(q != null)
  require(server != null)

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val buf = msg.asInstanceOf[ByteBuf]
    if (buf.isGet) {
      val writeBuf = ctx.alloc().buffer()
      try ctx.writeAndFlush(read(buf, writeBuf.retain()))
      finally writeBuf.release()
    } else {
      val res =
        if (buf.isPut) write(buf)
        else if (buf.isShutdown) shutdown
        else if (buf.isQuit) quit
        else Buffers.InvalidReq
      ctx.writeAndFlush(res)
    }
  }

  private def shutdown = ok {
    log.info("shutting down server")
    server.close()
  }

  private def quit = ok {
    log.info("closing connection")
    connection.close().sync()
  }

  private def write(buf: ByteBuf) = if (isValid(buf)) ok(q.write(buf.moveToPayloadIndex)) else Buffers.InvalidReq

  private def isValid(buf: ByteBuf) = (1 to buf.moveToPayloadIndex.readableBytes).forall(_ => AllowedChars.contains(buf.readByte))

  private def read(buf: ByteBuf, writeBuf: ByteBuf) = {
    val str = buf.moveToPayloadIndex.toString(Buffers.commandOffset, buf.readableBytes, US_ASCII)
    if (NumberUtils.isDigits(str)) {
      val n = str.toInt
      if (n > 0) {
        val wroteCount = q.read(n, to = writeBuf)
        if (wroteCount > 0) writeBuf
        else Buffers.Error
      }
      else {
        log.error("{} should be positive integer > 0", n)
        Buffers.InvalidReq
      }
    } else {
      log.error("{} is not a valid integer", str)
      Buffers.InvalidReq
    }
  }

  @inline private def ok(ignore: Any) = Buffers.Ok
}

object NettyServerHandler {
  private val log = Logger(getClass)
}