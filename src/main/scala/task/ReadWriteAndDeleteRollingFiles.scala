package task

import java.util.concurrent.atomic.AtomicInteger

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder
import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}


object ReadWriteAndDeleteRollingFiles {

  val b    = new StringBuffer()
  val path = "D:/ververica_task/tmp"

  def main(args: Array[String]): Unit = {

    //  new File("""d:\ververica_task\tmp""").listFiles().sorted(
    //    Ordering.comparatorToOrdering(LastModifiedFileComparator.LASTMODIFIED_COMPARATOR)
    //  )

    val released = new AtomicInteger()
    val acquired = new AtomicInteger()

    val q = ChronicleQueue
      .singleBuilder(path)
      .maxTailers(1)
      .rollCycle(RollCycles.TEST_SECONDLY)
      .build()


    //    println(s"q.firstCycle() = ${q.firstCycle()}")
    //    println(s"q.lastCycle() = ${q.lastCycle()}")
    //    println(s"q.cycle() = ${q.cycle()}")
    //    println(s"q.indexCount() = ${q.indexCount()}")
    //    println(s"q.firstIndex() = ${q.firstIndex()}")

    //    val t = q.createTailer(defaultTailer)

    val t = SingleChronicleQueueBuilder
      .single(path)
      .storeFileListener(new ReaderListener(path))
      .build()
      .createTailer("default")

    //    println(s"t.index() = ${t.index()}")
    //    println(s"t.cycle() = ${t.cycle()}")
    val a = q.acquireAppender()
    //
    //    val diff = a.cycle() - t.cycle()
    //    println(s"diff = $diff")
    //
    //    val store = q.storeForCycle(a.cycle(), 0, false, null)
    //    println(s"q.storeForCycle(q.cycle(), 0, false, null) = $store")
    //    if (store != null) {
    //      println(s"store.file().getAbsolutePath = ${store.file().getAbsolutePath}")
    //    }

    //    sys.exit(0)

    var i      = 1
    val writer = new Thread(() => {
      while (true) {
        a.writeText(i.toString)

        i += 1
        printIt(s"wrote: $i")
        //        printIt(tailer.peekDocument())
        Thread.sleep(100)
      }
    })
    writer.setName("writer")

    val read   = new AtomicInteger()
    val reader = new Thread(() => {
      while (true) {
        printIt(s"${t.readText()}, read = ${read.incrementAndGet()}")
        printIt("---------------------------------")
        //        val doc = t.readingDocument()
        //        if (doc.isPresent) {
        //          val w = doc.wire()
        //          val text = w.asText()
        //          read += 1
        //          printIt(s"read:  $text")
        //          Thread.sleep(1000)
        //          doc.close()
        //        } else {
        //          Thread.sleep(1000)
        //        }
        Thread.sleep(100)
      }
    })
    reader.setName("reader")

    writer.start()
    reader.start()

    //    writer.join(15000)
    //    reader.join(15000)

    writer.join(10000)
    reader.join(10000)

    println("--------------------------------")
    println(b)

    sys.exit(1)
  }

  def printIt(s: String) = b.append(s).append('\n')

}
