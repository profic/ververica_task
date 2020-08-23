package task

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Future

import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import org.apache.commons.lang3.RandomStringUtils

object ConcurrentIssuesChronicleTest {

  val lock = new Object()

  def main(args: Array[String]): Unit = {
    val path  = """d:\ververica_task\tmp\""" + RandomStringUtils.randomAlphabetic(3)
    val queue = ChronicleQueue
      .singleBuilder(path)
      .maxTailers(100)
      .rollCycle(RollCycles.LARGE_HOURLY)
      //      .storeFileListener(listener)
      .build()

    val appender = queue.acquireAppender()

    appender.writeText("1")
    appender.writeText("2")
    appender.writeText("3")
    appender.writeText("4")
    appender.writeText("5")

    val t1 = queue.createTailer(RandomStringUtils.randomAlphabetic(3))
    val t2 = queue.createTailer(RandomStringUtils.randomAlphabetic(3))
    val t3 = queue.createTailer(RandomStringUtils.randomAlphabetic(3))

    import concurrent.ExecutionContext.Implicits.global

    //    println(s"d1.wire().getValueIn.text() = ${d1.wire().getValueIn.text()}")
    //    println(s"d2.wire().getValueIn.text() = ${d2.wire().getValueIn.text()}")
    //    println(s"d3.wire().getValueIn.text() = ${d3.wire().getValueIn.text()}")
    //
    //    d2.rollbackOnClose()
    //    d1.close()
    //    d2.close()
    //    d3.close()
    //
    //    val d4 = tailer.readingDocument()
    //    println(s"d4.wire().getValueIn.text() = ${d4.wire().getValueIn.text()}")
    //    d4.close()
    //
    //    sys.exit(0)
    //
    //    val i = new AtomicInteger()
    //
    //    def read = {
    //      val doc = tailer.readingDocument()
    //      if (doc.isPresent) {
    //        println(doc.wire().getValueIn.text())
    //      } else {
    //        println(s"NOT PRESENT ${i.incrementAndGet()}")
    //        ""
    //      }
    //    }
    //
    //    Future {
    //      read
    //    }

  }
}


//package task
//
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.atomic.AtomicInteger
//
//import scala.concurrent.Future
//
//import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
//import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
//import org.apache.commons.lang3.RandomStringUtils
//
//object ConcurrentIssuesChronicleTest {
//
//  val lock = new Object()
//
//  def main(args: Array[String]): Unit = {
//    val path = """d:\ververica_task\tmp\""" + RandomStringUtils.randomAlphabetic(3)
//    val queue = ChronicleQueue
//      .singleBuilder(path)
//      .maxTailers(100)
//      .rollCycle(RollCycles.LARGE_HOURLY)
//      //      .storeFileListener(listener)
//      .build()
//
//    val appender = queue.acquireAppender()
//    val tailer = queue.createTailer(RandomStringUtils.randomAlphabetic(3))
//
//    appender.writeText("1")
//    appender.writeText("2")
//    appender.writeText("3")
//    appender.writeText("4")
//    appender.writeText("5")
//
//    {
//      val d1 = tailer.readingDocument()
//      println(s"d1.wire().getValueIn.text() = ${d1.wire().getValueIn.text()}")
//      d1.close()
//
//      val d2 = tailer.readingDocument()
//      println(s"d2.wire().getValueIn.text() = ${d2.wire().getValueIn.text()}")
//      d2.close()
//
//      val d3 = tailer.readingDocument()
//      println(s"d3.wire().getValueIn.text() = ${d3.wire().getValueIn.text()}")
//      d3.close()
//
//      sys.exit(0)
//    }
//
//    import concurrent.ExecutionContext.Implicits.global
//
//    val latch1 = new CountDownLatch(1)
//    val latch2 = new CountDownLatch(1)
//
//
//    val d1 = tailer.readingDocument()
//    val d2 = tailer.readingDocument()
//    val d3 = tailer.readingDocument()
//
//    println(s"d1.wire().getValueIn.text() = ${d1.wire().getValueIn.text()}")
//    println(s"d2.wire().getValueIn.text() = ${d2.wire().getValueIn.text()}")
//    println(s"d3.wire().getValueIn.text() = ${d3.wire().getValueIn.text()}")
//
//    d2.rollbackOnClose()
//    d1.close()
//    d2.close()
//    d3.close()
//
//    val d4 = tailer.readingDocument()
//    println(s"d4.wire().getValueIn.text() = ${d4.wire().getValueIn.text()}")
//    d4.close()
//
//    sys.exit(0)
//
//    val i = new AtomicInteger()
//
//    def read = {
//      val doc = tailer.readingDocument()
//      if (doc.isPresent) {
//        println(doc.wire().getValueIn.text())
//      } else {
//        println(s"NOT PRESENT ${i.incrementAndGet()}")
//        ""
//      }
//    }
//
//    Future {
//      read
//    }
//
//    lock.synchronized {
//
//    }
//  }
//}
