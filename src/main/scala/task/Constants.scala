package task

import io.netty.buffer.Unpooled.unreleasableBuffer
import io.netty.util.concurrent.FastThreadLocal
//import java.lang.ThreadLocal.withInitial
import java.nio.charset.StandardCharsets.US_ASCII

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled.wrappedBuffer

object Constants {

  // todo: simplify
  val AllowedChars: Array[Byte] = (Array(' ') ++ ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z')).map(_.toByte)

  private def getBuf(bufName: String) = new FastThreadLocal[ByteBuf]() {
    override protected def initialValue(): ByteBuf = unreleasableBuffer(wrappedBuffer(bufName.getBytes(US_ASCII)))
  }

  val SHUTDOWN      = "SHUTDOWN\r\n"
  val QUIT          = "QUIT\r\n"
  val invalidReq    = "INVALID_REQUEST\r\n" // todo: delimeter?
  val ok            = "OK\r\n" // todo: empty string? delimeter?
  val Error         = "ERR\r\n"
  val Put           = "PUT "
  val Get           = "GET "
  val defaultTailer = "default"

  private val putBuf        = getBuf(Put)
  private val _getBuf       = getBuf(Get)
  private val shutdownBuf   = getBuf(SHUTDOWN)
  private val quitBuf       = getBuf(QUIT)
  private val invalidReqBuf = getBuf(invalidReq)
  private val okBUf         = getBuf(ok)
  private val errorBuf      = getBuf(Error)

  // todo: retainedDuplicate or retain
  def SHUTDOWN_BUF: ByteBuf = shutdownBuf.get.resetReaderIndex
  def QUIT_BUF: ByteBuf = quitBuf.get.resetReaderIndex
  def INVALID_REQUEST_BUF: ByteBuf = invalidReqBuf.get.resetReaderIndex
  def OK_BUF: ByteBuf = okBUf.get.resetReaderIndex
  def ErrorBuf: ByteBuf = errorBuf.get.resetReaderIndex
  def PUT_BUF: ByteBuf = putBuf.get.resetReaderIndex
  def GET_BUF: ByteBuf = _getBuf.get.resetReaderIndex
}
