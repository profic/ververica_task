package task

import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import org.apache.commons.lang3.RandomStringUtils

// todo: remove
object WriteAfterStop {

  def main(args: Array[String]): Unit = {
    val q: SingleChronicleQueue = net.openhft.chronicle.queue.ChronicleQueue
      .singleBuilder("D:/ververica_task/" + RandomStringUtils.randomAlphabetic(3))
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY) // todo: рассчитать RPS и описать
      .build

    val a = q.acquireAppender

    def writeIt(value: String): Unit = {
      val wd = a.writingDocument
      wd.wire.getValueOut.bytes(value.getBytes)
      wd.close()
    }

    writeIt("1")
    writeIt("2")
    writeIt("3")
    writeIt("4")
    writeIt("5")
    writeIt("6")

    val t1 = q.createTailer

    println(t1.readingDocument.wire.asText)
    Thread.sleep(1000)
    println(t1.readingDocument.wire.asText)

  }

}
