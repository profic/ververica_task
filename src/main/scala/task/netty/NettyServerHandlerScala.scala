package task.netty

import java.io.Closeable
import java.nio.charset.StandardCharsets.US_ASCII

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import net.openhft.chronicle.queue.{ExcerptAppender, ExcerptTailer}
import org.apache.commons.lang3.math.NumberUtils
import task.C._
import task.MyService._
import task.{CountingTailer, MyService, Quit, Unknown}

/**
 * Handler implementation for the echo server.
 */
@Sharable
class NettyServerHandlerScala(
    private val connection: SocketChannel,
    private val producer: ExcerptAppender,
    private val consumer: ExcerptTailer,
    private val countingTailer: CountingTailer,
    private val server: Closeable
) extends SimpleChannelInboundHandler[ByteBuf] {

  require(connection != null)
  require(producer != null)
  require(consumer != null)
  require(countingTailer != null)
  require(server != null)

  private val writeLock = new Object()
  private val readLock = new Object()

//  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
//    val buf = msg.asInstanceOf[ByteBuf]
//    val res = if (buf.isQuit) quit
//    else if (buf.isShutdown) shutdown
//    else invalidOrElse(buf.hasDataToRead) {
//      if (buf.isPut) write(buf)
//      else invalidOrElse(buf.isGet)(read(buf))
//    }
//    ctx.write(res)
//  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    val buf = msg.asInstanceOf[ByteBuf]
//    buf.toReqType match { // todo?
//      case task.Shutdown => shutdown
//      case Quit => quit
//      case task.Get =>
//      case task.Put =>
//      case Unknown =>
//    }
    val res = if (buf.isQuit) quit
    else if (buf.isShutdown) shutdown
    else invalidOrElse(buf.hasDataToRead) {
      if (buf.isPut) write(buf)
      else invalidOrElse(buf.isGet)(read(buf))
    }
    ctx.write(res)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = ctx.flush()

  private def shutdown = ok {
    MyService.log.debug("shutting down server")
    server.close()
  }

  private def quit = ok {
    MyService.log.debug("closing connection")
    connection.close().sync()
  }

  private def write(buf: ByteBuf) = invalidOrElse(isValid(buf)) {
    ok {
      writeLock.synchronized { // todo: synchronized
        val dc = producer.writingDocument
        try {
          val bytes = dc.wire.bytes
          (0 until buf.resetToFour.readableBytes).foreach(_ => bytes.writeByte(buf.readByte))
        }
        catch {
          case t: Throwable =>
            dc.rollbackOnClose()
            throw t
        } finally dc.close()

        countingTailer.increment()
      }
    }
  }

  // todo: collections without allocation?
  private def isValid(buf: ByteBuf) = (0 until buf.resetToFour.readableBytes).forall { _ =>
    arr.contains(buf.readByte.toChar)
  }

  private def read(buf: ByteBuf) = /* synchronized */ { // todo: synchronized
    val str = buf.resetToFour.toString(INDEX_FOUR, buf.readableBytes, US_ASCII)
    invalidOrElse(NumberUtils.isDigits(str)) {
      val n = str.toInt

      invalidOrElse(n > 0) {
        readLock.synchronized { // todo: synchronized
          if (countingTailer.available >= n) readIt(n)
          else ErrorBuf
        }
      }
    }
  }

  private def readIt(n: Int) = {
    val buf = b
    (1 to n).foreach { _ =>
      val doc = consumer.readingDocument
      try {
        if (doc.isPresent) {
          val by = doc.wire().bytes()
          (0 until by.length()).foreach(_ => buf.writeByte(by.readByte()))
          buf.writeByte('\n')
        } else {
          throw new IllegalStateException("") // todo
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          doc.rollbackOnClose()
          throw e
      } finally {
        doc.close()
      }

      countingTailer.decrement()
    }

    buf.writeByte('\r').writeByte('\n')
  }

  private def b = Unpooled.buffer() // todo: def???

  @inline private def ok(ignore: Any) = OK_BUF

  @inline private def invalidOrElse(b: Boolean)(f: => ByteBuf) = if (b) f else INVALID_REQUEST_BUF
}
