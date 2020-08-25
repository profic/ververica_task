package task.server

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.{Config, ConfigFactory}
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.{ServerSocketChannel, SocketChannel}
import io.netty.handler.codec.LineBasedFrameDecoder
import task.store.Queue

object NettyTcpServer {

  val config: Config = ConfigFactory.load()

  def main(args: Array[String]): Unit = {
    val path = config.getString("workDir")
    newServer(10042, Queue(path))
  }

  def newServer(port: Int, q: Queue): Closeable = {
    val factory      = if (Epoll.isAvailable) EpollFactory else NioFactory
    val bossGroup    = factory.eventLoopGroup(1)
    val workerGroup  = factory.eventLoopGroup()
    val serverFuture = new AtomicReference[ChannelFuture]()

    def shutdownServer(): Unit = {
      serverFuture.get().cancel(true)
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
            .addLast(new LineBasedFrameDecoder(config.getInt("max-line-size")))
            .addLast(new NettyServerHandler(ch, q, closeServer))
        }
      })
      .channelFactory(factory.channelFactory)

    sys.addShutdownHook(shutdownServer())

    serverFuture.set(server.bind(port).sync())
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
