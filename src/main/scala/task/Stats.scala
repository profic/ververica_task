package task

import org.HdrHistogram.Histogram

object Stats {

  val hCountingTailer = new Histogram(5)
  val hReadIt = new Histogram(5)
  val hToEndIndex = new Histogram(5)
  val hCountExcerpts = new Histogram(5)

  def main(args: Array[String]): Unit = {
    val histogram = new Histogram(5)

    histogram.recordValue(11)
    histogram.recordValue(100)
    histogram.recordValue(15)
    histogram.recordValue(30)
    histogram.recordValue(50)
    histogram.recordValue(70)

    println(histogram.getMean)
    println(s"histogram.getValueAtPercentile(50) = ${histogram.getValueAtPercentile(50)}")
    println(s"histogram.getValueAtPercentile(75) = ${histogram.getValueAtPercentile(75)}")
    println(s"histogram.getValueAtPercentile(99) = ${histogram.getValueAtPercentile(99)}")
  }

}
