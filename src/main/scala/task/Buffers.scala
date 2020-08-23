package task

import java.nio.charset.StandardCharsets.US_ASCII

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled.{unreleasableBuffer, wrappedBuffer}
import io.netty.util.concurrent.FastThreadLocal

object Buffers {

  private def getBuf(bufName: String) = new FastThreadLocal[ByteBuf]() {
    override protected def initialValue(): ByteBuf = unreleasableBuffer(wrappedBuffer(bufName.getBytes(US_ASCII)))
  }

  private val putBuf        = getBuf(Constants.Put)
  private val _getBuf       = getBuf(Constants.Get)
  private val shutdownBuf   = getBuf(Constants.Shutdown)
  private val quitBuf       = getBuf(Constants.Quit)
  private val invalidReqBuf = getBuf(Constants.InvalidReq)
  private val okBUf         = getBuf(Constants.Ok)
  private val errorBuf      = getBuf(Constants.Error)

  // todo: retainedDuplicate or retain
  def Shutdown: ByteBuf = shutdownBuf.get.resetReaderIndex
  def Quit: ByteBuf = quitBuf.get.resetReaderIndex
  def InvalidReq: ByteBuf = invalidReqBuf.get.resetReaderIndex
  def Ok: ByteBuf = okBUf.get.resetReaderIndex
  def Error: ByteBuf = errorBuf.get.resetReaderIndex
  def Put: ByteBuf = putBuf.get.resetReaderIndex
  def Get: ByteBuf = _getBuf.get.resetReaderIndex

}
