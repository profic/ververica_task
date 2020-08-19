package task

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue

case class CountingTailer(private val queue: SingleChronicleQueue) {

  private final val tailer = queue.createTailer

  def entryCount: Long = /* synchronized */ { // todo: remove? /* synchronized */?
    val lastIndex = end
    if (lastIndex == 0) 0
    else queue.countExcerpts(queue.firstIndex, lastIndex)
  }

  def end: Long = /* synchronized */(tailer.toEnd.index) // todo: /* synchronized */

}
