package task

import java.util.concurrent.atomic.AtomicLong

import net.openhft.chronicle.queue.ExcerptTailer
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue

class CountingTailer(queue: SingleChronicleQueue, consumer: ExcerptTailer) {

  def decrement(): Unit = count.decrementAndGet()

  def increment(): Unit = count.incrementAndGet()

  private final val count = {
    val consumerIndex = consumer.index
    val lastIndex = consumer.toEnd.index

    val count = (consumerIndex, lastIndex) match {
      case (0, 0) => 0
      case (0, _) => queue.countExcerpts(queue.firstIndex, lastIndex)
      case _ => lastIndex - consumerIndex
    }

    new AtomicLong(count)
  }

  def available: Long = count.get()

}
