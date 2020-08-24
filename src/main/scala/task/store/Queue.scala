package task.store

import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.Logger
import io.netty.buffer.ByteBuf
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.{ChronicleQueue, ExcerptAppender, ExcerptTailer, RollCycles}
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import task.{Buffers, Constants}
import task.Ops.repeat
import task.Buffers.ByteBufOps

object Queue {
  def apply(path: String) = new Queue(
    ChronicleQueue
      .singleBuilder(path)
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY)
      .build()
  )
}

class Queue(q: SingleChronicleQueue) {

  private val log = Logger(getClass)

  private val writeLock = new Object()
  private val readLock  = new Object()

  private val producer    : ExcerptAppender = q.acquireAppender()
  private val consumer                      = q.createTailer(Constants.defaultTailerName).disableThreadSafetyCheck(true)
  private val messageCount: MessageCount    = new MessageCount()

  def close(): Unit = q.close()

  def write(buf: ByteBuf): Unit = writeLock.synchronized {
    val doc = producer.writingDocument
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

    messageCount.increment()
  }

  def read(n: Int, to: ByteBuf): Int = readLock.synchronized {
    if (messageCount.available >= n) readIt(n, to)
    else 0
  }

  /*
      todo: read operations supposed to be quick, so additional EventExecutorGroup not needed
   */
  private def readIt(n: Int, buf: ByteBuf) = {
    repeat(n) {
      val doc = consumer.readingDocument
      try {
        if (doc.isPresent) {
          val bytes = doc.wire().bytes()
          repeat(times = bytes.length) {
            buf.writeByte(bytes.readByte())
          }
          buf.addNewLine()
        } else {
          throw new IllegalStateException("Document should exist. This can indicate an bug in MessageCount")
        }
      } catch {
        case t: Throwable =>
          doc.rollbackOnClose()
          log.error("Error while reading message", t)
          throw t
      }

      messageCount.decrement()
    }

    buf.writeByte('\r').addNewLine().readableBytes()
  }

  /**
   * For testing purposes only!
   */
  private[task] def drainAll(): Unit = readLock.synchronized {
    messageCount.reset()
    consumer.toEnd
  }

  private class MessageCount {

    def decrement(): Unit = count.decrementAndGet()
    def increment(): Unit = count.incrementAndGet()

    private final val count = {
      val countingTailer = q.createTailer(randomAlphanumeric(10)).disableThreadSafetyCheck(true)
      try {
        val consumerIndex = consumer.index
        val lastIndex     = countingTailer.toEnd.index

        val count = (consumerIndex, lastIndex) match {
          case (0, 0) => 0
          case (0, _) => q.countExcerpts(q.firstIndex, lastIndex)
          case _      => lastIndex - consumerIndex
        }

        new AtomicLong(count)
      } finally countingTailer.close()
    }

    def available: Long = count.get()

    def reset(): Unit = count.set(0)
  }

}
