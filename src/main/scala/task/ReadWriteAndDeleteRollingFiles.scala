package task

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.util.Comparator

import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.StoreFileListener
import net.openhft.chronicle.queue.impl.single.{SingleChronicleQueue, SingleChronicleQueueBuilder}
import org.apache.commons.io.comparator.LastModifiedFileComparator


object ReadWriteAndDeleteRollingFiles {

  new File("""d:\ververica_task\tmp""").listFiles().sorted(
    Ordering.comparatorToOrdering(LastModifiedFileComparator.LASTMODIFIED_COMPARATOR)
  )

  def main(args: Array[String]): Unit = {
    val listener = new StoreFileListener() {
      override def onReleased(cycle: Int, file: File): Unit = {
        //          println("---------------------------------")
        //          val bool = file.deleteOnExit()
        //          println(s"delete file = $file, success = $bool")
        println(s"onReleased file = $file")
      }

      override def onAcquired(cycle: Int, file: File): Unit = {
                  println(s"onAcquired file = ${file}")
        //          println("----- ----------------------------")
      }
    }

    val path = "D:/ververica_task/tmp"
    val q = net.openhft.chronicle.queue.ChronicleQueue
      .singleBuilder(path)
      .maxTailers(1)
      .rollCycle(RollCycles.TEST_SECONDLY)
//      .storeFileListener(listener)
      .build()

    val a = q.acquireAppender()

    //    val t = q.createTailer("default")
    val t = SingleChronicleQueueBuilder.binary(path).storeFileListener(listener).build().createTailer()

    var i = 1
    val writer = new Thread(() => {
      while (true) {
        a.writeText(i.toString)

        i += 1
        //        println(s"wrote: $i")
        //        println(tailer.peekDocument())
        Thread.sleep(400)
      }
    })

    var read = 0
    val reader = new Thread(() => {
      while (true) {
        println(s"t.readingDocument().isPresent = ${t.readingDocument().isPresent}")
        //        val doc = t.readingDocument()
        //        if (doc.isPresent) {
        //          val w = doc.wire()
        //          val text = w.asText()
        //          read += 1
        //          println(s"read:  $text")
        //          Thread.sleep(1000)
        //          doc.close()
        //        } else {
        //          Thread.sleep(1000)
        //        }
        Thread.sleep(500)
      }
    })

    writer.start()
    reader.start()

    writer.join(6000)
    reader.join(6000)

    sys.exit(1)
  }


}
