package task

import java.io.File
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

import com.twitter.finagle.builder.ServerBuilder
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.{ClientConnection, ListeningServer, ServiceFactory}
import com.twitter.logging.Logger
import com.twitter.util.{Await, Duration, Future, Time}
import io.netty.buffer.{ByteBuf, Unpooled}
import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import net.openhft.chronicle.queue.impl.StoreFileListener
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import org.apache.commons.lang3.RandomStringUtils
import task.finagle.StringServer.server

object PutAllTogether {
  val listener = new StoreFileListener() {
    override def onReleased(cycle: Int, file: File): Unit = {
      println(s"onReleased file = $file")
    }

    override def onAcquired(cycle: Int, file: File): Unit = {
      println(s"onAcquired file = ${file}")
    }
  }

  val path = "/tmp/" + RandomStringUtils.randomAlphabetic(3)
  val queue: SingleChronicleQueue = ChronicleQueue
    .singleBuilder(path)
    .maxTailers(1)
    .rollCycle(RollCycles.LARGE_HOURLY)
    //      .storeFileListener(listener)
    .build()


  val producer = queue.acquireAppender()
  val consumer =
    queue
      //    SingleChronicleQueueBuilder.binary(path)
      //      .storeFileListener(listener)
      //      .build()
      .createTailer()

  val countingTailer = CountingTailer(queue)

  private[this] val log = Logger.get()


  def main(args: Array[String]): Unit = {

    var serv: ListeningServer = null
    val underlyingFactory: ServiceFactory[String, String] = new ServiceFactory[String, String] {

      private[this] val producer = queue.acquireAppender()
      private[this] val consumer = queue.createTailer("default")
      private[this] val countingTailer = CountingTailer(queue)

      def apply(conn: ClientConnection): Future[MyService] = Future.value {
        new MyService(serv, conn, producer, consumer, countingTailer) // todo: check if it is possible to make service local and reassign connection
      }

      override def close(deadline: Time): Future[Unit] = Future.Done
    }

    serv = ServerBuilder()
      .name("task")
      .stack(server)
      .reportTo(NullStatsReceiver)
      .tracer(NullTracer)
      .configured((Transport.Options(noDelay = true, reuseAddr = true, reusePort = true), Transport.Options.param)) // todo: reusePort – enables or disables SO_REUSEPORT option on a transport socket (Linux 3.9+ only).
      .bindTo(new InetSocketAddress("localhost", 8080))
      .build(underlyingFactory)

    sys.runtime.addShutdownHook(OnShuttingDown)

    object OnShuttingDown extends Thread(() => {
      val serverClose = serv.close(Duration.fromSeconds(4))
      Await.result(serverClose)
    })
  }
}

object C {

  val SHUTDOWN = "SHUTDOWN"
  val QUIT = "QUIT"
  val invalidRequest = "INVALID_REQUEST"
  val ok = "OK"
  val Error = "ERR\r\n"

  val SHUTDOWN_BUF: ByteBuf = Unpooled.copiedBuffer(SHUTDOWN, StandardCharsets.US_ASCII)
  val QUIT_BUF: ByteBuf = Unpooled.copiedBuffer(QUIT, StandardCharsets.US_ASCII)
}