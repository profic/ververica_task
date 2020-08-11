package task

import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue

object WriteAfterStop {

  def main(args: Array[String]): Unit = {
    val q: SingleChronicleQueue = net.openhft.chronicle.queue.ChronicleQueue
      .singleBuilder("D:\\ververica_task")
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY) // todo: рассчитать RPS и описать
      .build


    val a = q.acquireAppender

    def writeIt(value: String): Unit = {
      val wd = a.writingDocument
      wd.wire.getValueOut.bytes(value.getBytes)
      wd.close()
    }

    //    writeIt("1")
    //    writeIt("2")
    //    writeIt("3")
    //    writeIt("4")
    //    writeIt("5")
    //    writeIt("6")

    val t1 = q.createTailer

    var doc = t1.readingDocument

    while (doc.isPresent) {
      println(doc.wire.asText)
      doc.close()
      doc = t1.readingDocument
    }
  }

}
