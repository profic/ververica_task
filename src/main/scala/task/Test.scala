package task

import scala.util.Try

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.{ExcerptTailer, RollCycles}
import net.openhft.chronicle.wire.Wire
import org.apache.commons.lang3.RandomStringUtils
import task.C.defaultTailer

object Test {

  def main(args: Array[String]): Unit = {
    val q: SingleChronicleQueue = net.openhft.chronicle.queue.ChronicleQueue
      .singleBuilder("D:\\ververica_task\\tmp")
      .maxTailers(1)
      .rollCycle(RollCycles.MINUTELY)
      .build()

    val a = q.acquireAppender()
    val t = q.createTailer(defaultTailer)
//    val t2 = q.createTailer(defaultTailer)
//
//    a.writeText("string1")
//    a.writeText("string2")
//
//    println(s"t.readText() = ${t.readText()}")
//    println(s"t2.readText() = ${t2.readText()}")
//
//    sys.exit(0)

//    val i = "test_"
    def i = {
      RandomStringUtils.randomAlphabetic(5) + "_"
    }

    qwe(w => w.getValueOut.writeString(i + "getValueOut.writeString"))
    qwe(w => w.write(i + "write"))

    qwe(w => w.write.bytes((i + "write.bytes").getBytes))
    qwe(w => w.write.rawBytes((i + "write.rawBytes").getBytes))
    qwe(w => w.write.rawText(i + "write.rawText"))
    qwe(w => w.write.text(i + "write.text"))

    qwe(w => w.getValueOut.bytes((i + "getValueOut.bytes").getBytes))

    qwe(w => w.getValueOut.rawBytes((i + "getValueOut.rawBytes").getBytes))
    qwe(w => w.getValueOut.rawText(i + "getValueOut.rawText"))
    qwe(w => w.getValueOut.text(i + "getValueOut.text"))

    qwe(w => w.writeText(i + "writeText"))

    a.writeText(i + "a.writeText")
    readIt(t)

    def qwe(f: Wire => Unit): Unit = {
      val wd = a.writingDocument()
      f(wd.wire())
      wd.close()

      readIt(t)

      println("---------------------------------------------------------------------------")
    }

  }

  private def readIt(t: ExcerptTailer) = {
//    Try(println(s"t.readText() = ${t.readText()}"))

    val doc = t.readingDocument()

    val w = doc.wire()
    if (w != null) {
      Try(println(s"w.asText() = ${w.asText()}"))
      Try(println(s"w.readText() = ${w.readText()}"))
      Try(println(s"w.read().`object`() = ${w.read().`object`()}"))
      Try(println(s"w.read().text() = ${w.read().text()}"))
      Try(println(s"w.read().readString() = ${w.read().readString()}"))
      Try(println(s"w.read().bytes() = ${new String(w.read().bytes())}"))
    }

    doc.close()
  }
}