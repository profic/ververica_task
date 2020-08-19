package task.netty

import java.io.Closeable
import java.nio.charset.StandardCharsets.US_ASCII

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import net.openhft.chronicle.bytes.BytesOut
import net.openhft.chronicle.queue.{ExcerptAppender, ExcerptTailer}
import org.apache.commons.lang3.math.NumberUtils
import task.C._
import task.MyService._
import task.{C, CountingTailer, MyService}

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

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = /* synchronized */ { // todo: /* synchronized */
//    val buf = msg.asInstanceOf[ByteBuf]
//    ctx.writeAndFlush(apply(buf))
//    "Hello".getBytes.foreach { b =>
//      ctx.write(b)
//    }


    ctx.write("hello")
    ctx.flush()
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = ctx.flush

  def apply(buf: ByteBuf): ByteBuf = {
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
    connection.close().sync()
  }

  private def write(buf: ByteBuf) = invalidOrElse(isValid(buf)) {
    ok {
      //      println("START WRITING")
      val res = buf.resetToFour
      //      producer.wire().write(res.toString(INDEX_FOUR, res.readableBytes(), US_ASCII))
      producer.writeText(res.toString(INDEX_FOUR, res.readableBytes(), US_ASCII))

      //      producer.writeBytes { (bytes: BytesOut[_]) => // todo: omit closure?
      //        //        (0 until buf.resetToFour.readableBytes).foreach(_ => bytes.writeByte(buf.readByte)) // todo
      //        (0 until buf.resetToFour.readableBytes).foreach { _ =>
      //          val byt = buf.readByte
      //          //          print(byt.toChar)
      //          bytes.writeByte(byt)
      //        }
      //      }
    }
  }

  // todo: collections without allocation?
  private def isValid(buf: ByteBuf) = (0 until buf.resetToFour.readableBytes).forall { _ =>
    arr.contains(buf.readByte.toChar)
  }

  private def read(buf: ByteBuf) = /* synchronized */ { // todo: /* synchronized */
    val str = buf.resetToFour.toString(INDEX_FOUR, buf.readableBytes, US_ASCII)
    invalidOrElse(NumberUtils.isDigits(str)) {
      val n = str.toInt

      invalidOrElse(n > 0) {
        synchronized {
          val end = countingTailer.end
          val consumerIndex = consumer.index
          if (consumerIndex == 0) {
            if (countingTailer.entryCount >= n) readIt(n)
            else ErrorBuf
          }
          else if (end != 0 && end - consumerIndex >= n) readIt(n)
          else ErrorBuf
        }
      }
    }
  }


  //  private def readIt(n: Int) = {
  //    val sb = new StringBuilder
  //    (1 to n).foreach { _ =>
  //      {
  //        val doc = consumer.readingDocument
  //        try {
  //          if (doc.isPresent) {
  //            val text = doc.wire().asText()
  //            println(s"doc.wire().asText() = $text")
  //            doc.close()
  //            sb.append(text)
  //          } else {
  //            throw new IllegalStateException("") // todo
  //          }
  //        } catch {
  //          case _: Exception => doc.rollbackOnClose()
  //        }
  //      }
  //
  //      {
  //        val doc = consumer.readingDocument
  //        try {
  //          if (doc.isPresent) {
  //            val text = doc.wire().read().`object`()
  //            println(s"doc.wire().read().`object`() = ${text}")
  //            doc.close()
  //            sb.append(text)
  //          } else {
  //            throw new IllegalStateException("") // todo
  //          }
  //        } catch {
  //          case _: Exception => doc.rollbackOnClose()
  //        }
  //      }
  //
  //      {
  //        val doc = consumer.readingDocument
  //        try {
  //          if (doc.isPresent) {
  //            val text = doc.wire().asText()
  //            doc.close()
  //            sb.append(text)
  //          } else {
  //            throw new IllegalStateException("") // todo
  //          }
  //        } catch {
  //          case _: Exception => doc.rollbackOnClose()
  //        }
  //      }
  //
  //      {
  //        val doc = consumer.readingDocument
  //        try {
  //          if (doc.isPresent) {
  //            val text = doc.wire().asText()
  //            doc.close()
  //            sb.append(text)
  //          } else {
  //            throw new IllegalStateException("") // todo
  //          }
  //        } catch {
  //          case _: Exception => doc.rollbackOnClose()
  //        }
  //      }
  //
  //      {
  //        val doc = consumer.readingDocument
  //        try {
  //          if (doc.isPresent) {
  //            val text = doc.wire().asText()
  //            doc.close()
  //            sb.append(text)
  //          } else {
  //            throw new IllegalStateException("") // todo
  //          }
  //        } catch {
  //          case _: Exception => doc.rollbackOnClose()
  //        }
  //      }
  //    }
  //
  //    sb.append('\r').append('\n')
  //
  //    //    Unpooled.copiedBuffer(sb.toString, US_ASCII)
  //    val buf = b.retainedDuplicate() // todo: ???
  //    buf.writeCharSequence(sb, US_ASCII)
  //    buf
  //  }

  def readIt(n: Int) = {
    val sb = new java.lang.StringBuilder
    val tmp = new java.lang.StringBuilder
    (1 to n).foreach { _ =>
      val doc = consumer.readingDocument
      try {
        if (doc.isPresent) {
          //          val text = doc.wire().asText()
          //          val text = new String(doc.wire.getValueIn.bytes(), US_ASCII)
          doc.wire.getValueIn.text(tmp)
          sb.append(tmp).append('\n')
        }
        //        else {
        //          throw new IllegalStateException("") // todo
        //        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          doc.rollbackOnClose()
      } finally {
        doc.close()
      }
    }

    sb.append('\r').append('\n')

    //    Unpooled.copiedBuffer(sb.toString, US_ASCII)
    val buf = b.retainedDuplicate() // todo: ???
    buf.writeCharSequence(sb, US_ASCII)
    buf
  }

  private def b = Unpooled.buffer() // todo: def???

  @inline private def ok(f: => Unit) = {
    f
    OK_BUF
  }

  @inline private def invalidOrElse(b: Boolean)(f: => ByteBuf) = if (b) f else INVALID_REQUEST_BUF
}
