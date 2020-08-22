package task.netty

import java.io.Closeable
import java.nio.charset.StandardCharsets

import com.twitter.util.{Await, Closable, Duration, Future, Time}
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import net.openhft.chronicle.bytes.BytesUtil
import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import org.apache.commons.lang3.RandomStringUtils
import task.C._
import task.CountingTailer

/**
 * Echoes back any received data from a client.
 */
object NettyServerScala {

  def main(args: Array[String]): Unit = {
    //    val path = "d:\\ververica_task\\gatling\\" + RandomStringUtils.randomAlphabetic(3)
    //    val path = """d:\ververica_task\gatling"""
    //
    //    val queue: SingleChronicleQueue = ChronicleQueue
    //      .singleBuilder(path)
    //      .maxTailers(1)
    //      .rollCycle(RollCycles.LARGE_HOURLY)
    //      //      .storeFileListener(listener)
    //      .build()
    //
    //    newServer(10042, queue)

    serverForGatling()
    //  val q = queueForGatling


  }

  def serverForGatling(): Unit = {
    val queue: SingleChronicleQueue = queueForGatling

    //    newServer(8080, queue)
    newServer(10042, queue)
  }

  def queueForGatling: SingleChronicleQueue = {
        val path = """d:\ververica_task_prepared_data"""
//    val path = """d:\ververica_task_prepared_data_text"""

    val queue: SingleChronicleQueue = ChronicleQueue
      .singleBuilder(path)
      .maxTailers(100)
      .rollCycle(RollCycles.LARGE_HOURLY)
      //      .storeFileListener(listener)
      .build()

    queue
  }

  def newServer(port: Int, q: SingleChronicleQueue) = {
    val bossGroup = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup

//            val tailer = q.createTailer(defaultTailer) // todo
    val tailer = q.createTailer(RandomStringUtils.randomAlphanumeric(10))
    val countingTailer = new CountingTailer(q, tailer)
    val appender = q.acquireAppender()
    val sF: Closeable = () => {
      bossGroup.shutdownGracefully().sync();
      workerGroup.shutdownGracefully().sync();
      Future.Done
    }

    val b = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .option(ChannelOption.SO_BACKLOG, Integer.valueOf(100)) // todo
      .childHandler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline
            .addLast(new LineBasedFrameDecoder(1000000)) // todo: line len
            .addLast(new NettyServerHandlerScala(ch, appender, tailer, countingTailer, sF))
        }
      })
      .channelFactory(new ChannelFactory[ServerSocketChannel] {
        def newChannel(): ServerSocketChannel = try {
          new EpollServerSocketChannel()
        } catch {
          case _: UnsatisfiedLinkError => new NioServerSocketChannel()
        }
      })
    // Start the server.
    val f = b.bind(port).sync() // todo
    // Wait until the server socket is closed.

    sys.runtime.addShutdownHook(OnShuttingDown)
    object OnShuttingDown extends Thread(() => {
      bossGroup.shutdownGracefully().sync();
      workerGroup.shutdownGracefully().sync();
    })

    //    f.channel.closeFuture.sync()  // todo
  }
}
