package task

import java.nio.charset.StandardCharsets.US_ASCII

import io.netty.buffer.{ByteBuf, ByteBufUtil}
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

  val commandOffset = 4

  implicit class ByteBufOps(val buf: ByteBuf) extends AnyVal {
    def isShutdown: Boolean = ByteBufUtil.equals(buf, Buffers.Shutdown)
    def isQuit: Boolean = ByteBufUtil.equals(buf, Buffers.Quit)
    def isPut: Boolean = ByteBufUtil.equals(buf, 0, Buffers.Put, 0, commandOffset) && hasDataToRead
    def isGet: Boolean = ByteBufUtil.equals(buf, 0, Buffers.Get, 0, commandOffset) && hasDataToRead

    def moveToPayloadIndex: ByteBuf = buf.readerIndex(commandOffset)

    def addNewLine(): ByteBuf = buf.writeByte('\n')

    private def hasDataToRead: Boolean = buf.readableBytes > commandOffset
  }

}
