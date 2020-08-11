package task

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import scala.util.Random

import net.openhft.chronicle.queue.{ChronicleQueue, ExcerptAppender}
import net.openhft.chronicle.wire.Wire
import org.apache.commons.lang3.RandomStringUtils

object createDirs {
  def main(args: Array[String]): Unit = {
    val m = List(
      "getValueOut.bytes",
      "getValueOut.rawText",
      "getValueOut.text",
      "writeText",
      "a.writeText",
    )

    m.map(o => s"""D:/ververica_task/small_$o""").foreach(p => Files.createDirectory(Paths.get(p)))
    m.map(o => s"""D:/ververica_task/large_$o""").foreach(p => Files.createDirectory(Paths.get(p)))
  }
}

object SizeTest {
  val iter = 3 * 1000 * 1000

  /*
    string : small/large
    methods: 1/2/3/4/5
     - getValueOut.bytes
     - getValueOut.rawText
     - getValueOut.text
     - writeText
     - a.writeText
  */

  val base = """D:/ververica_task/"""

  def main(args: Array[String]): Unit = {

    def qwe(f: (String, Wire) => Unit)(a: ExcerptAppender, s: String): Unit = {
      val wd = a.writingDocument()
      f(s, wd.wire())
      wd.close()
    }

    val m = List(
      ("getValueOut.writeString", qwe((s, w) => w.getValueOut.writeString(s)) _),
      ("getValueOut.rawBytes", qwe((s, w) => w.getValueOut.rawBytes(toAsciString(s))) _),
      ("getValueOut.rawText", qwe((s, w) => w.getValueOut.rawText(s)) _),

      ("write", qwe((s, w) => w.write(s)) _),
      ("write.rawBytes", qwe((s, w) => w.write.rawBytes(toAsciString(s))) _),
      ("write.rawText", qwe((s, w) => w.write.rawText(s)) _),
      ("write.text", qwe((s, w) => w.write.text(s)) _),

      ("write", qwe((s, w) => w.writeText(s)) _),

      ("getValueOut.rawText", qwe((s, w) => w.getValueOut.rawText(s)) _),
      ("getValueOut.text", qwe((s, w) => w.getValueOut.text(s)) _),
      ("getValueOut.bytes", qwe((s, w) => w.getValueOut.bytes(toAsciString(s))) _),
      ("writeText", qwe((s, w) => w.writeText(s)) _)
      //        , ("a.writeText", qwe((s, w) => w.getValueOut.bytes(s)) _),
    )

    val paths = m.map(o => s"""${base}small_${o._1}""") ::: m.map(o => s"""${base}large_${o._1}""")

    val sizes = List(
      "small",
      "large"
    )

    m.foreach {
      case (method, f) =>
        sizes.foreach { s =>
          val path = s"""${base}${s}_$method"""
          val q = ChronicleQueue.singleBuilder(path).maxTailers(1).build()
          val a = q.acquireAppender()
          val size = s match {
            case "small" => 50
            case "large" => 1000
          }

          val start = System.currentTimeMillis()
          (1 to iter).foreach { _ =>
            f(a, RandomStringUtils.randomAlphanumeric(size))
          }
          val elapsed = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)
          println(s"${q.fileAbsolutePath()} time: $elapsed")
        }
    }

    sizes.foreach { s =>
      val path = s"""${base}${s}_a.writeText"""
      val q = ChronicleQueue.singleBuilder(path).maxTailers(1).build()
      val a = q.acquireAppender()
      val size = s match {
        case "small" => 50
        case "large" => 1000
      }

      val start = System.currentTimeMillis()
      //          val iter = 10 * 1000 * 1000
      (1 to iter).foreach { _ =>
        a.writeText(RandomStringUtils.randomAlphanumeric(size))
      }
      val elapsed = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)
      println(s"${q.fileAbsolutePath()} time: $elapsed")
    }
  }

  @inline
  private def toAsciString(s: String) = {
    s.getBytes(StandardCharsets.US_ASCII)
  }
}
