package task

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue

case class CountingTailer(private val q: SingleChronicleQueue) {

  private final val tailer = q.createTailer

  def entryCount: Long = { // todo: remove?
    tailer.toEnd
    val lastIndex = tailer.index
    if (lastIndex == 0) return 0
    q.countExcerpts(q.firstIndex, lastIndex)
  }

  def end: Long = {
    tailer.toEnd
    tailer.index
  }

}
