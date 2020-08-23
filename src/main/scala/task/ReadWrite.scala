package task

import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue

object ReadWrite {

  def main(args: Array[String]): Unit = {
    val q: SingleChronicleQueue = net.openhft.chronicle.queue.ChronicleQueue
      .singleBuilder("D:\\ververica_task")
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY) // todo: рассчитать RPS и описать
      .build()

    val a = q.acquireAppender()

    var i      = 1000000000L
    val writer = new Thread(() => {
      while (true) {
        val wd = a.writingDocument()

        wd.wire().getValueOut.bytes(i.toString.getBytes())
        wd.close()

        i += 1
        if (i % 100000 == 0) {
          println(s"wrote: $i")
          Thread.sleep(1)
        }
      }
    })

    val t = q.createTailer("default123")

    var read   = 0
    val reader = new Thread(() => {
      while (true) {
        val doc = t.readingDocument()
        if (doc.isPresent) {
          val w    = doc.wire()
          val text = w.asText()
          read += 1
          if (read % 100000 == 0) {
            println(s"read:  $text")
          }
          doc.close()
        } else {
          Thread.sleep(1)
        }
      }
    })

    writer.start()
    reader.start()

    writer.join()
    reader.join()
  }


}
