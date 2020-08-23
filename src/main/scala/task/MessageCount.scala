package task

import java.util.concurrent.atomic.AtomicLong

import net.openhft.chronicle.queue.ExcerptTailer
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric

class MessageCount(queue: SingleChronicleQueue, consumer: ExcerptTailer) {

  def decrement(): Unit = count.decrementAndGet()
  def increment(): Unit = count.incrementAndGet()

  private final val count = {
    val countingTailer = queue.createTailer(randomAlphanumeric(10)).disableThreadSafetyCheck(true)
    try {
      val consumerIndex = consumer.index
      val lastIndex     = countingTailer.toEnd.index

      val count = (consumerIndex, lastIndex) match {
        case (0, 0) => 0
        case (0, _) => queue.countExcerpts(queue.firstIndex, lastIndex)
        case _      => lastIndex - consumerIndex
      }

      new AtomicLong(count)
    } finally countingTailer.close()
  }

  def available: Long = count.get()

}
