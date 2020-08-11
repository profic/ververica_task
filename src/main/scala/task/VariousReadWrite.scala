package task

import scala.util.Try

import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.{ExcerptTailer, RollCycles}
import net.openhft.chronicle.wire.Wire

object VariousReadWrite {

  def main(args: Array[String]): Unit = {
    val q: SingleChronicleQueue = net.openhft.chronicle.queue.ChronicleQueue
      .singleBuilder("D:\\ververica_task\\tmp")
      .maxTailers(1)
      .rollCycle(RollCycles.MINUTELY)
      .build()

    val a = q.acquireAppender()
    val t = q.createTailer("default")

    var cnt = 1
    def i = {
      val s = s"${cnt}_test_"
      cnt += 1
      s
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
    }

  }

  private def readIt(t: ExcerptTailer) = {
    //    Try(println(s"t.readText() = ${t.readText()}"))

    val doc = t.readingDocument()

    val w = doc.wire()
    if (w != null) {
      Try(println(s"1 w.asText() = ${w.asText()}"))
      Try(println(s"2 w.readText() = ${w.readText()}"))
      Try(println(s"3 w.read().`object`() = ${w.read().`object`()}"))
      Try(println(s"4 w.read().text() = ${w.read().text()}"))
      Try(println(s"5 w.read().readString() = ${w.read().readString()}"))
      Try(println(s"6 w.read().bytes() = ${new String(w.read().bytes())}"))

      println()
      println("------------------------------")
      println()
    }

    doc.close()
  }
}
