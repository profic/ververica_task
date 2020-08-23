package task

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.US_ASCII
import java.util.concurrent.atomic.AtomicInteger

import scala.util.Try

import net.openhft.chronicle.bytes.Bytes
import net.openhft.chronicle.bytes.pool.BytesPool
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.{ExcerptTailer, RollCycles}
import net.openhft.chronicle.wire.{ValueIn, ValueOut, Wire}
import org.apache.commons.lang3.RandomStringUtils
import task.Constants.defaultTailer

object VariousReadWrite {

  private val pool = new BytesPool

  def main(args: Array[String]): Unit = {
    val q: SingleChronicleQueue = net.openhft.chronicle.queue.ChronicleQueue
      .singleBuilder("D:/ververica_task/tmp/" + RandomStringUtils.randomAlphanumeric(5))
      .maxTailers(1)
      .rollCycle(RollCycles.MINUTELY)
      .build

    val a = q.acquireAppender
    val t = q.createTailer(defaultTailer)

    var cnt = 1

    def i = {
      val s = s"${cnt}_test_"
      cnt += 1
      s
    }

    qwe(w => w.getValueOut.writeString(i + "getValueOut.writeString"))
    qwe(w => w.write(i + "write"))

    qwe(w => w.write.bytes((i + "write.bytes").getBytes(US_ASCII)))
    qwe(w => w.write.rawBytes((i + "write.rawBytes").getBytes(US_ASCII)))
    qwe(w => w.write.rawText(i + "write.rawText"))
    qwe(w => w.write.text(i + "write.text"))

    qwe(w => w.getValueOut.bytes((i + "getValueOut.bytes").getBytes(US_ASCII)))
    qwe(w => w.getValueOut.rawBytes((i + "getValueOut.rawBytes").getBytes(US_ASCII)))
    qwe(w => w.getValueOut.rawText(i + "getValueOut.rawText"))
    qwe(w => w.getValueOut.text(i + "getValueOut.text"))

    qwe(w => w.writeText(i + "writeText"))

    readIt(t, a.writeText(i + "a.writeText"))

    def qwe(f: Wire => Unit): Unit = {
      readIt(t, {
        val wd = a.writingDocument
        f(wd.wire)
        wd.close()
      })

      {
        //        val wd = a.writingDocument
        //        f(wd.wire)
        //        wd.close()
        //        val doc = t.readingDocument
        //        val w = doc.wire
        //        println(s"w.asText = ${w.asText}")
      }
    }

  }

  private def readIt(t: ExcerptTailer, write: => Unit) = {
    //    Try(println(s"t.readText = ${t.readText}"))

    val i = new AtomicInteger(1)

    def qwe(f: Wire => String) = {
      Try {
        write
        t.readingDocument
      }.map { doc =>
        Try {
          val w = doc.wire
          println(s"${i.getAndIncrement()} ${f(w)}")
        }
        doc.close()
      }.recover {
        case e => e.printStackTrace()
      }
    }

    def pooled(s: String, f: Wire => Bytes[_] => Any) = {
      val value = pool.acquireBytes
      qwe { w =>
        f(w)
          .apply(value)
        s"$s = $value"
      }
      value
    }

    qwe(w => {
      val sequence = w.asText()
      s"w.asText = $sequence"
    })
    qwe(w => {
      val str = w.readText()
      s"w.readText = $str"
    }) // StringBuilder sb = Wires.acquireStringBuilder(); dc.wire().getValueIn().text(sb);
    qwe(w => {
      val read  = w.read
      val value = read.`object`()
      s"w.read.`object` = $value"
    }) // readField(acquireStringBuilder(), null, ANY_CODE_MATCH.code());
    qwe(w => {
      val read = w.read
      val str  = read.text()
      s"w.read.text = $str"
    })
    qwe(w => {
      val read = w.read
      val str  = read.readString()
      s"w.read.readString = $str"
    })
    qwe(w => {
      val read  = w.read
      val bytes = read.bytes()
      s"w.read.bytes new String = ${new String(bytes)}"
    })
    qwe(w => {
      val read  = w.read
      val value = read.bytesStore()
      s"w.read.bytesStore = $value"
    })
    pooled("w.read.bytes = ", w => {
      val read = w.read
      we => {
        read.bytes(we)
      }
    })
    pooled("w.read.text = ", w => {
      val read = w.read
      we => {
        read.text(we)
      }
    })
    pooled("in.bytes = ", w => {
      val in = w.getValueIn
      we => {
        in.bytes(we)
      }
    })
    pooled("in.text = ", w => {
      val in = w.getValueIn
      we => {
        in.text(we)
      }
    })
    qwe(w => {
      val in  = w.getValueIn
      val str = in.text()
      s"w.getValueIn.text = $str"
    })
    qwe(w => {
      val in    = w.getValueIn
      val value = in.`object`()
      s"w.getValueIn.object = $value"
    })
    qwe(w => {
      val in  = w.getValueIn
      val str = in.readString()
      s"w.getValueIn.readString = $str"
    })


    //      qwe(w => s"w.getValueIn. = ${w.getValueIn.}")
    //      qwe(w => s"w.getValueIn. = ${w.getValueIn.`object`}")
    //      qwe(w => s"w.getValueIn. = ${w.getValueIn.`object`}")

    println
    println("------------------------------")
    println
  }

}
