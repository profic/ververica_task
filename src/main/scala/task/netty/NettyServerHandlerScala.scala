package task.netty

import java.io.Closeable
import java.nio.charset.StandardCharsets.US_ASCII

import com.typesafe.scalalogging.Logger
import io.netty.buffer.{ByteBuf, ByteBufUtil}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import net.openhft.chronicle.queue.{ExcerptAppender, ExcerptTailer}
import org.apache.commons.lang3.math.NumberUtils
import task.Constants._
import task.MessageCount

object NettyServerHandlerScala {

  private val writeLock = new Object()
  private val readLock  = new Object()

  private val log = Logger(getClass)

  val INDEX_FOUR = 4 // todo: name

  implicit class ByteBufOps(val buf: ByteBuf) extends AnyVal {
    def isShutdown: Boolean = compareBuffers(SHUTDOWN_BUF)
    def isQuit: Boolean = compareBuffers(QUIT_BUF)
    def isPut: Boolean = ByteBufUtil.equals(buf, 0, PUT_BUF, 0, INDEX_FOUR) && hasDataToRead
    def isGet: Boolean = ByteBufUtil.equals(buf, 0, GET_BUF, 0, INDEX_FOUR) && hasDataToRead

    def resetToFour: ByteBuf = buf.readerIndex(INDEX_FOUR) // todo: name
    def hasDataToRead: Boolean = buf.readableBytes > INDEX_FOUR

    // todo
    def toReqType: ReqType =
      if (isQuit) Quit
      else if (isShutdown) Shutdown
      else if (isPut) Put
      else if (isGet) Get
      else Unknown

    private def compareBuffers(compareWith: ByteBuf) = ByteBufUtil.equals(buf, compareWith)
  }

}

class NettyServerHandlerScala(
  private val connection: SocketChannel,
  private val producer: ExcerptAppender,
  private val consumer: ExcerptTailer,
  private val countingTailer: MessageCount,
  private val server: Closeable
) extends SimpleChannelInboundHandler[ByteBuf] {

  import NettyServerHandlerScala._

  require(connection != null)
  require(producer != null)
  require(consumer != null)
  require(countingTailer != null)
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
        else INVALID_REQUEST_BUF
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
  private def isValid(buf: ByteBuf) = (0 until buf.resetToFour.readableBytes).forall(_ => AllowedChars.contains(buf.readByte))

  private def read(buf: ByteBuf, writeBuf: ByteBuf) = {
//    Thread.sleep(1000) // todo: remove
    val str = buf.resetToFour.toString(INDEX_FOUR, buf.readableBytes, US_ASCII)
    invalidOrElse(NumberUtils.isDigits(str)) {
      val n = str.toInt

      invalidOrElse(n > 0) {
        readLock.synchronized { // todo: synchronized
          if (countingTailer.available >= n) readIt(n, writeBuf)
          else ErrorBuf
        }
      }
    }
  }

  private def readIt(n: Int, buf: ByteBuf) = {
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

  @inline private def ok(ignore: Any) = OK_BUF

  @inline private def invalidOrElse(b: Boolean)(f: => ByteBuf) = if (b) f else INVALID_REQUEST_BUF
}

// todo?
sealed trait ReqType
case object Shutdown extends ReqType
case object Quit extends ReqType
case object Get extends ReqType
case object Put extends ReqType
case object Unknown extends ReqType
// todo?