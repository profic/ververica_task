package task.netty

import java.io.Closeable

import com.twitter.util.Future
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.handler.codec.LineBasedFrameDecoder
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import org.apache.commons.lang3.RandomStringUtils
import task.MessageCount

object NettyServerScala {

  def main(args: Array[String]): Unit = {
    serverForGatling()
  }

  def serverForGatling(): Unit = {
    val queue = queueForGatling

    //    newServer(8080, queue)
    newServer(10042, queue)
  }

  def queueForGatling: SingleChronicleQueue = {
    val path = """d:\ververica_task_prepared_data"""
    //    val path = """d:\ververica_task_prepared_data_text"""

    ChronicleQueue
      .singleBuilder(path)
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY)
      .build()
  }

  def newServer(port: Int, q: SingleChronicleQueue) = {
    val factory     = if (Epoll.isAvailable) EpollFactory else NioFactory
    val bossGroup   = factory.eventLoopGroup(1)
    val workerGroup = factory.eventLoopGroup()

    //            val tailer = q.createTailer(defaultTailer).disableThreadSafetyCheck(true) // todo
    val tailer         = q.createTailer(RandomStringUtils.randomAlphanumeric(10)).disableThreadSafetyCheck(true)
    val countingTailer = new MessageCount(q, tailer)
    val appender       = q.acquireAppender()
    val sF: Closeable  = () => {
      bossGroup.shutdownGracefully().sync()
      workerGroup.shutdownGracefully().sync()
      q.close()
      Future.Done
    }

    // todo
    //    bootstrap.setOption("tcpNoDelay", true);
    //    bootstrap.setOption("keepAlive", true);

    val b = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .option(ChannelOption.SO_BACKLOG, Integer.valueOf(100)) // todo
      .childHandler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline
            //            .addLast(new LoggingHandler()) // todo
            .addLast(new LineBasedFrameDecoder(1000000)) // todo: line len
            .addLast(new NettyServerHandlerScala(ch, appender, tailer, countingTailer, sF))
        }
      })
      .channelFactory(factory.channelFactory)
    val f = b.bind(port).sync() // todo
    // Wait until the server socket is closed.

    sys.runtime.addShutdownHook(new Thread(() => {
      bossGroup.shutdownGracefully().sync()
      workerGroup.shutdownGracefully().sync()
      q.close()
    }))

    //    f.channel.closeFuture.sync()  // todo
  }
}

trait Factory {
  final def channelFactory: ChannelFactory[ServerSocketChannel] = () => channel
  protected def channel: ServerSocketChannel
  def eventLoopGroup(threads: Int = 0): EventLoopGroup
}

object EpollFactory extends Factory {
  override protected def channel: ServerSocketChannel = new EpollServerSocketChannel
  override def eventLoopGroup(threads: Int = 0): EventLoopGroup = new EpollEventLoopGroup(threads)
}

object NioFactory extends Factory {
  override protected def channel: ServerSocketChannel = new NioServerSocketChannel
  override def eventLoopGroup(threads: Int = 0): EventLoopGroup = new NioEventLoopGroup(threads)
}
