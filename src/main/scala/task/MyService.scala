package task

import java.nio.charset.StandardCharsets.US_ASCII
import java.util.regex.Pattern

import com.twitter.finagle.Service
import com.twitter.logging.Logger
import com.twitter.util.{Closable, Future}
import io.netty.buffer.{ByteBuf, ByteBufUtil, Unpooled}
import net.openhft.chronicle.bytes.BytesOut
import net.openhft.chronicle.bytes.pool.BytesPool
import net.openhft.chronicle.queue.{ExcerptAppender, ExcerptTailer}
import org.apache.commons.lang3.math.NumberUtils
import task.C._

object MyService {
  val log: Logger = Logger.get
  val p: Pattern = Pattern.compile("[A-Za-z0-9]+")

  implicit class ByteBufOps(val buf: ByteBuf) extends AnyVal {
    def isShutdown: Boolean = compareBuffers(SHUTDOWN_BUF)

    def isQuit: Boolean = compareBuffers(QUIT_BUF)

    def isPut: Boolean = ByteBufUtil.equals(buf.resetReaderIndex, 0, PUT_BUF, 0, INDEX_FOUR)

    def isGet: Boolean = ByteBufUtil.equals(buf.resetReaderIndex, 0, GET_BUF, 0, INDEX_FOUR)

    def resetToFour: ByteBuf = buf.readerIndex(INDEX_FOUR) // todo: name

    def hasDataToRead: Boolean = buf.readableBytes > INDEX_FOUR

    def toReqType: ReqType =
      if (isQuit) Quit
      else if (isShutdown) Shutdown
      else if (isPut) Put
      else if (isGet) Get
      else Unknown

    private def compareBuffers(compareWith: ByteBuf) = ByteBufUtil.equals(buf.resetReaderIndex, compareWith)
  }

}

// todo?
sealed trait ReqType
case object Shutdown extends ReqType
case object Quit extends ReqType
case object Get extends ReqType
case object Put extends ReqType
case object Unknown extends ReqType
// todo?

import task.MyService._

class MyService(
    private val server: Closable,
    private val connection: Closable,
    private val producer: ExcerptAppender,
    private val consumer: ExcerptTailer,
    private val countingTailer: CountingTailer,
    private val pool: BytesPool
) extends Service[ByteBuf, ByteBuf] {

  import C._

  override def apply(buf: ByteBuf): Future[ByteBuf] = Future.value {
    if (buf.isQuit) quit
    else if (buf.isShutdown) shutdown
    else invalidOrElse(buf.readableBytes > INDEX_FOUR) {
      if (buf.isPut) write(buf)
      else invalidOrElse(buf.isGet)(read(buf))
    }
  }

  private def shutdown = ok {
    MyService.log.debug("shutting down server")
    server.close()
  }

  private def quit = ok {
    MyService.log.debug("closing connection")
    connection.close()
  }

  private def write(buf: ByteBuf) = invalidOrElse(isValid(buf)) {
    ok {
//      println("START WRITING")
      producer.writeBytes { (bytes: BytesOut[_]) =>
        //        (0 until buf.resetToFour.readableBytes).foreach(_ => bytes.writeByte(buf.readByte)) // todo
        (0 until buf.resetToFour.readableBytes).foreach { _ =>
          val byt = buf.readByte
//          print(byt.toChar)
          bytes.writeByte(byt)
        }
      }
    }
  }

  // todo: collections without allocation?
  private def isValid(buf: ByteBuf) = (0 until buf.resetToFour.readableBytes).forall { _ =>
    arr.contains(buf.readByte.toChar)
  }

//  private def read(buf: ByteBuf) = {
//    val str = buf.resetToFour.toString(INDEX_FOUR, buf.readableBytes, US_ASCII)
//    invalidOrElse(NumberUtils.isDigits(str)) {
//      val n = str.toInt
//
//      invalidOrElse(n > 0) {
//        val end = countingTailer.end
//        if (consumer.index == 0) {
//          if (countingTailer.entryCount >= n) readIt(n)
//          else ErrorBuf
//        }
//        else if (end != 0 && end - consumer.index >= n) readIt(n)
//        else ErrorBuf
//      }
//    }
//  }

  private def read(buf: ByteBuf) = {
    val str = buf.resetToFour.toString(INDEX_FOUR, buf.readableBytes, US_ASCII)
    val n = str.toInt
    readIt(n)
  }

  private def readIt(n: Int) = {
    val sb = new StringBuilder
    (1 to n).foreach { _ =>
      val doc = consumer.readingDocument
      if (doc.isPresent) {
        val text = doc.wire.bytes.toString
        doc.close()
        sb.append(text).append('\n')
      } else {
        throw new IllegalStateException("") // todo
      }
    }

    sb.append('\r').append('\n')

    //    Unpooled.copiedBuffer(sb.toString, US_ASCII)
    val buf = b.retainedDuplicate() // todo: ???
    buf.writeCharSequence(sb, US_ASCII)
    buf
  }

  private val b = Unpooled.buffer() // todo: ???

  @inline private def ok(f: => Unit) = { f; OK_BUF }

  @inline private def invalidOrElse(b: Boolean)(f: => ByteBuf) = if (b) f else INVALID_REQUEST_BUF
}