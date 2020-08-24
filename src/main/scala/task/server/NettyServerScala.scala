package task.server

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
import task.store.{MessageCount, Queue}

object NettyServerScala {

  // todo: remove
  def main(args: Array[String]): Unit = {
    serverForGatling()
  }

  def serverForGatling(): Unit = {

    //    newServer(8080, queue)
    //    val path = """d:\ververica_task_prepared_data"""
    val path = """d:\ververica_task"""
    newServer(10042, Queue(path))
  }

  def newServer(port: Int, q: Queue): Closeable = {
    var serverFuture: ChannelFuture = null
    val factory                     = if (Epoll.isAvailable) EpollFactory else NioFactory
    val bossGroup                   = factory.eventLoopGroup(1)
    val workerGroup                 = factory.eventLoopGroup()

    def shutdownServer(): Unit = {
      serverFuture.cancel(true)
      bossGroup.shutdownGracefully().sync()
      workerGroup.shutdownGracefully().sync()
      q.close()
    }

    val closeServer: Closeable = () => shutdownServer()

    val server = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .childHandler(new ChannelInitializer[SocketChannel]() {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline
            .addLast(new LineBasedFrameDecoder(1000000)) // todo: line len
            .addLast(new NettyServerHandlerScala(ch, q, closeServer))
        }
      })
      .channelFactory(factory.channelFactory)

    sys.addShutdownHook(shutdownServer())

    serverFuture = server.bind(port).sync() // todo
    closeServer
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
