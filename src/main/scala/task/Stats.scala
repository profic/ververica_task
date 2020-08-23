package task

import org.HdrHistogram.Histogram

object Stats {

  val hCountingTailer = new Histogram(5)
  val hReadIt         = new Histogram(5)
  val hToEndIndex     = new Histogram(5)
  val hCountExcerpts  = new Histogram(5)

}
