package task.netty

import java.nio.charset.StandardCharsets

import com.twitter.util.{Await, Closable, Duration, Future, Time}
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
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
    //    val path = """d:\ververica_task_prepared_data"""
    val path = """d:\ververica_task_prepared_data_text"""

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

    //        val tailer = q.createTailer(defaultTailer) // todo
    val tailer = q.createTailer(RandomStringUtils.randomAlphanumeric(10))
    val countingTailer = CountingTailer(q)
    val appender = q.acquireAppender()
    val sF: java.io.Closeable = () => {
      bossGroup.shutdownGracefully().sync();
      workerGroup.shutdownGracefully().sync();
      Future.Done
    }

    //    val h = new NettyServerHandlerScala(null, appender, tailer, countingTailer, sF)
    //
    //    {
    //      import concurrent.ExecutionContext.Implicits.global
    //
    //      (0 to 4).map { _ =>
    //        scala.concurrent.Future {
    //          var i = 0
    //          while (true) {
    //            try {
    //              val res = h.readIt(1)
    //              i += 1
    //              if (i % 100000 == 0) {
    //                println(Thread.currentThread().getName + ", res = " + res.toString(StandardCharsets.US_ASCII))
    //              }
    //            } catch {
    //              case e: Throwable =>
    //                e.printStackTrace()
    //                throw e
    //            }
    //          }
    //        }
    //      }.foreach(sd => scala.concurrent.Await.ready(sd, scala.concurrent.duration.Duration.Inf))
    //    }


    //    // todo: remove
    //    {
    //      // todo: remove
    //      var i = 0
    //
    //      while (true) {
    //        val res = h.readIt(1)
    //        i += 1
    //        if (i % 100000 == 0) {
    //          println("res = " + res.toString(StandardCharsets.US_ASCII))
    //        }
    //      }
    //    }

    val b = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .option(ChannelOption.SO_BACKLOG, Integer.valueOf(100))
      //        .handler(new LoggingHandler(LogLevel.INFO))

      .handler(new ChannelHandler {
        override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
          println("handlerAdded")
        }

        override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
          println("handlerRemoved")
        }

        override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
          println("exceptionCaught")
        }
      })

      .childHandler(new ChannelInitializer[SocketChannel]() {

        override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
          super.channelRead(ctx, msg)
        }

        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline
            .addLast(new LineBasedFrameDecoder(1000000)) // todo: line len
            .addLast(new NettyServerHandlerScala(ch, appender, tailer, countingTailer, sF))
//            .addLast(new LoggingHandler(LogLevel.INFO));
        }
      })
      .channelFactory(new ChannelFactory[ServerSocketChannel] {
        def newChannel() = try {
          new io.netty.channel.epoll.EpollServerSocketChannel()
        } catch {
          case _: UnsatisfiedLinkError => new NioServerSocketChannel()
        }
      })
    // Start the server.
    val f = b.bind(port).sync()
    // Wait until the server socket is closed.

    sys.runtime.addShutdownHook(OnShuttingDown)
    object OnShuttingDown extends Thread(() => {
      bossGroup.shutdownGracefully().sync();
      workerGroup.shutdownGracefully().sync();
    })

    //    f.channel.closeFuture.sync()
  }
}
