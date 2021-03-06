package task.store

import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.Logger
import io.netty.buffer.ByteBuf
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import task.Buffers.ByteBufOps
import task.Constants
import task.Ops.repeat

object Queue {
  def apply(path: String): Queue = {
    val readerListener = new ReaderListener(path)
    val queue          = ChronicleQueue
      .singleBuilder(path)
      .maxTailers(1)
      .storeFileListener(readerListener)
      .rollCycle(RollCycles.LARGE_HOURLY)
      .build()
    new Queue(queue)
  }
}

class Queue(q: SingleChronicleQueue) {

  private val log = Logger(getClass)

  private val readLock = new Object()

  private val consumer = q.createTailer(Constants.DefaultTailerName).disableThreadSafetyCheck(true)

  private final val msgCount = {
    val tailer = q.createTailer(randomAlphanumeric(10)).disableThreadSafetyCheck(true)
    try {
      val consumerIndex = consumer.index
      val lastIndex     = tailer.toEnd.index

      val count = (consumerIndex, lastIndex) match {
        case (0, 0) => 0
        case (0, _) => q.countExcerpts(q.firstIndex, lastIndex)
        case _      => lastIndex - consumerIndex
      }

      new AtomicLong(count)
    } finally tailer.close()
  }

  def close(): Unit = q.close()

  def write(buf: ByteBuf): Unit = {
    val doc = q.acquireAppender().writingDocument
    try {
      val bytes = doc.wire.bytes
      repeat(times = buf.readableBytes) {
        bytes.writeByte(buf.readByte)
      }
    } catch {
      case t: Throwable =>
        doc.rollbackOnClose()
        log.error("Error while writing message", t)
        throw t
    } finally doc.close()

    increaseMsgCount()
  }

  def read(n: Int, to: ByteBuf): Int = readLock.synchronized {
    if (available >= n) {
      repeat(n) {
        val doc = consumer.readingDocument
        try {
          if (doc.isPresent) {
            val bytes = doc.wire().bytes()
            repeat(times = bytes.length) {
              to.writeByte(bytes.readByte())
            }
            to.addNewLine()
          } else {
            throw new IllegalStateException("Document should exist. This can indicate an bug in MessageCount")
          }
        } catch {
          case t: Throwable =>
            doc.rollbackOnClose()
            log.error("Error while reading message", t)
            throw t
        }

        decreaseMsgCount()
      }

      to.writeByte('\r').addNewLine().readableBytes()
    }
    else 0
  }

  /**
   * For testing purposes only!
   */
  private[task] def drainAll(): Unit = readLock.synchronized {
    reset()
    consumer.toEnd
  }

  private def decreaseMsgCount(): Unit = msgCount.decrementAndGet()
  private def increaseMsgCount(): Unit = msgCount.incrementAndGet()
  private def available: Long = msgCount.get()
  private def reset(): Unit = msgCount.set(0)
}
