package task

import java.io.File
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.US_ASCII

import com.twitter.finagle.builder.{ServerBuilder, ServerConfig}
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.{ClientConnection, ListeningServer, ServiceFactory}
import com.twitter.logging.Logger
import com.twitter.util._
import io.netty.buffer.{ByteBuf, Unpooled}
import net.openhft.chronicle.bytes.pool.BytesPool
import net.openhft.chronicle.queue.impl.StoreFileListener
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import org.apache.commons.lang3.RandomStringUtils
import task.C.defaultTailer
import task.finagle.Server

class MyServiceFactory(queue: SingleChronicleQueue, server: => Closable) extends ServiceFactory[ByteBuf, ByteBuf] {

  private val appender = queue.acquireAppender()
  private val tailer = queue.createTailer(defaultTailer)
  private val countingTailer = CountingTailer(queue)
  private val pool = new BytesPool()

  def apply(conn: ClientConnection): Future[MyService] = create(conn)

  def apply(conn: Closable): Future[MyService] = create(conn) // todo: not need?

  private def create(conn: Closable) = Future.value {
    new MyService(server, conn, appender, tailer, countingTailer, pool) // todo: check if it is possible to make service local and reassign connection
  }


  override def close(deadline: Time): Future[Unit] = Future.Done
}

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

    //    (1 to 1000).foreach { i =>
    //      producer.writeText(s"$i blabla")
    //    }

    val host = "localhost"
    val port = 10042

    val serv = apply(host, port).proceed(queue)

    sys.runtime.addShutdownHook(OnShuttingDown)
    object OnShuttingDown extends Thread(() => {
      val serverClose = serv.close(Duration.fromSeconds(4))
      Await.result(serverClose)
    })
  }

  def apply(host: String, port: Int) = {
    ServerBuilder()
      .name("task")
      .stack(Server())
      .reportTo(NullStatsReceiver)
      .tracer(NullTracer)
      .configured((Transport.Options(noDelay = true, reuseAddr = true, reusePort = true), Transport.Options.param)) // todo: reusePort – enables or disables SO_REUSEPORT option on a transport socket (Linux 3.9+ only).
      .bindTo(new InetSocketAddress(host, port))
  }

  implicit class ServOps(b: ServerBuilder[ByteBuf, ByteBuf, ServerConfig.Yes, ServerConfig.Yes, ServerConfig.Yes]) {
    def proceed(queue: SingleChronicleQueue): ListeningServer = {
      var serv: ListeningServer = null
      val underlyingFactory = new MyServiceFactory(queue, serv)
      serv = b.build(underlyingFactory)

      sys.runtime.addShutdownHook(OnShuttingDown)
      object OnShuttingDown extends Thread(() => {
        val serverClose = serv.close(Duration.fromSeconds(4))
        Await.result(serverClose)
      })
      serv
    }
  }

}

//class ServerClosable(var serv: Closable) extends Closable { // todo
//  def setServer(serv: Closable): Unit = this.serv = serv
//
//  override def close(deadline: Time): Future[Unit] = serv.close()
//}

object C {


  val arr = Array(' ') ++ ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z') // todo: simplify

  //  val SHUTDOWN = "SHUTDOWN\n"
  //  val QUIT = "QUIT\n"
  //  val invalidRequest = "INVALID_REQUEST\n" // todo: delimeter?
  //  val ok = "OK\n" // todo: empty string? delimeter?

  val SHUTDOWN = "SHUTDOWN\r\n"
  val QUIT = "QUIT\r\n"
  val invalidRequest = "INVALID_REQUEST\r\n" // todo: delimeter?
  val ok = "OK\r\n" // todo: empty string? delimeter?
  val Error = "ERR\r\n"

  val Put = "PUT "
  val Get = "GET "
  val INDEX_FOUR = 4

  val defaultTailer = "default"

//  private def putBuf = Unpooled.copiedBuffer(Put, US_ASCII)
//  private def getBuf = Unpooled.copiedBuffer(Get, US_ASCII)
//  private def shutdownBuf = Unpooled.copiedBuffer(SHUTDOWN, US_ASCII)
//  private def quitBuf = Unpooled.copiedBuffer(QUIT, US_ASCII)
//  private def invalidRequestBuf = copiedBuffer(invalidRequest, US_ASCII)
//  private def okBUf = Unpooled.copiedBuffer(ok, US_ASCII)
//  private def errorBuf = Unpooled.copiedBuffer(Error, US_ASCII)

  private def getBuf(put: String): ByteBuf = Unpooled.wrappedBuffer(put.getBytes(US_ASCII))

  private val putBuf = getBuf(Put)
  private val _getBuf = getBuf(Get)
  private val shutdownBuf = getBuf(SHUTDOWN)
  private val quitBuf = getBuf(QUIT)
  private val invalidRequestBuf = getBuf(invalidRequest)
  private val okBUf = getBuf(ok)
  private val errorBuf = getBuf(Error)

  def SHUTDOWN_BUF: ByteBuf = shutdownBuf
//    .resetReaderIndex() // todo: def
  def QUIT_BUF: ByteBuf = quitBuf
//    .resetReaderIndex() // todo: def
  def INVALID_REQUEST_BUF: ByteBuf = invalidRequestBuf.retainedDuplicate()
//    .resetWriterIndex().resetReaderIndex() // todo: def
  def OK_BUF: ByteBuf = okBUf.retainedDuplicate()
//    .resetWriterIndex().resetReaderIndex() // todo: def
  def ErrorBuf: ByteBuf = errorBuf.retainedDuplicate()
//    .resetWriterIndex().resetReaderIndex() // todo: def
  def PUT_BUF: ByteBuf = putBuf
//    .resetReaderIndex()
  def GET_BUF: ByteBuf = _getBuf
//    .resetReaderIndex()
}
