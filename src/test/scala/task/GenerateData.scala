package task

import net.openhft.chronicle.bytes.BytesOut
import net.openhft.chronicle.queue.RollCycles
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder

object GenerateData {

  def main(args: Array[String]): Unit = {
    generateText()
  }

  def generateBytes(): Unit = {
    val path = "d:\\ververica_task_prepared_data"

    val queue = SingleChronicleQueueBuilder
      //      .fieldlessBinary(new File(path))
      .single(path)
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY)
      .build()

    val appender = queue.acquireAppender()
    var i        = 1
    while (true) {
      appender.writeBytes((bytes: BytesOut[_]) => {
        val word = NumberToWords(i)
        s"$i $word".foreach(ch => bytes.writeByte(ch.toByte))
        if (i % 100000 == 0) println(i)
        i += 1
      })
    }
  }

  def generateText(): Unit = {
    val path = "d:\\ververica_task_prepared_data_text"

    val queue = SingleChronicleQueueBuilder
      //      .fieldlessBinary(new File(path))
      .single(path)
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY)
      .build()

    val appender = queue.acquireAppender()
    var i        = 1
    while (true) {
      val word = NumberToWords(i)
      appender.writeText(s"$i $word")
      if (i % 100000 == 0) {
        println(i)
      }
      i += 1
    }
  }
}
