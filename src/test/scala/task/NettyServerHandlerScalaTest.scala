package task

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.US_ASCII

import io.netty.buffer.{ByteBufUtil, Unpooled}
import io.netty.buffer.Unpooled.copiedBuffer
import io.netty.channel.socket.SocketChannel
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import org.mockito.Mockito
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

import org.mockito.Mockito._
import task.netty.{NettyServerHandlerScala, NettyServerScala}


object NettyServerHandlerScalaTest {

  def main(args: Array[String]): Unit = {
    val q = NettyServerScala.queueForGatling
    val tailer = q.createTailer(randomAlphabetic(3))
    val h: NettyServerHandlerScala = new NettyServerHandlerScala(
      mock(classOf[SocketChannel]),
      q.acquireAppender(),
      tailer,
      new CountingTailer(q, tailer),
      () => {}
    )

    val bytes = "GET 1".getBytes(US_ASCII)

//    println(
//      new String(h.channelRead0 {
//        copiedBuffer(bytes)
//      }.array(), US_ASCII)
//    )
//
//    println(
//      new String(h {
//        copiedBuffer(bytes)
//      }.array(), US_ASCII)
//    )

    List(1
            , 2, 3, 4
    ).map { _ =>
      Future {
        var i = 0
//        while (i != 200000) {
        while (true) {
          try {
//            val res = h {
//              copiedBuffer(bytes)
//            }
//
//            i += 1
//            if (i % 10000 == 0) {
//              println(Thread.currentThread().getName + ", res = " + new String(res.array(), US_ASCII))
//            }
          } catch {
            case _: Throwable =>
          }
        }
      }
    }.foreach(sd => Await.ready(sd, Duration.Inf))

    //    println("hReadIt")
    //    printStat(Stats.hReadIt)
    //    println("hCountingTailer")
    //    printStat(Stats.hCountingTailer)

    //    println("hToEndIndex")
    //    printStat(Stats.hToEndIndex)
    //    println("hCountExcerpts")
    //    printStat(Stats.hCountExcerpts)

  }

  private def printStat(h: org.HdrHistogram.Histogram): Unit = {
    println(s"h.getValueAtPercentile(50) = ${h.getValueAtPercentile(50)}")
    println(s"h.getValueAtPercentile(75) = ${h.getValueAtPercentile(75)}")
    println(s"h.getValueAtPercentile(99) = ${h.getValueAtPercentile(99)}")
    println(s"h.getValueAtPercentile(99.9) = ${h.getValueAtPercentile(99.9)}")
    println(s"h.getMean = ${h.getMean}")
  }
}
