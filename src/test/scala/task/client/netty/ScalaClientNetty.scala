package task.client.netty

import java.nio.charset.StandardCharsets.US_ASCII
import java.util.concurrent.{Executors, TimeUnit}

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled.copiedBuffer
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import io.netty.util.concurrent.{Future, GenericFutureListener}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise, Future => SFuture}


object ScalaClientNetty {

  def main(args: Array[String]): Unit = {

    val client = new ScalaClientNetty("localhost", 10042)
    Await.ready(client.startClient(), 5.seconds)
    client.writeMessage("GET 3\n").sync().addListener(
      (future: Future[_ >: Void]) => {
//        if (future.isDone) {
//          println(future)
//        }
        println("sync().addListener")
      }
    )
    //        Thread.sleep(1000);
    //        client.writeMessage("GET 1\r\n");
    //        client.stopClient();  //call this at some point to shutdown the client
  }
}

class ScalaClientNetty(host: String, port: Int) extends Runnable {
  private      val ready: Promise[Unit] = Promise()
  private      val clientHandler        = new ScalaNettyClientHandler(ready)
  private lazy val executor             = Executors.newFixedThreadPool(1)

  private var isRunning = false

  def startClient(): SFuture[Unit] = synchronized {
    if (!isRunning) {
      executor.execute(this)
      isRunning = true
    }
    ready.future
  }

  def stopClient: Boolean = synchronized {
    var bReturn = true
    if (isRunning) {
      executor.shutdown()
      try {
        executor.shutdownNow
        if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
          if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            bReturn = false
          }
        }
      } catch {
        case _: InterruptedException =>
          executor.shutdownNow()
          Thread.currentThread.interrupt()
      }
      isRunning = false
    }
    bReturn
  }

  override def run(): Unit = {
    val workerGroup = new NioEventLoopGroup()
    try { //            bootstrap.setOption("sendBufferSize", 1048576);
      //            bootstrap.setOption("receiveBufferSize", 1048576);
      //            bootstrap.setOption("writeBufferHighWaterMark", 10 * 64 * 1024);
      /*
                  I am not sure if "tcpNoDelay" helps to improve the throughput. Delay is there to improve the performance.
                  None the less, I tried it and saw that the throughput actually fell more than 90%.
                   */
      //            bootstrap.setOption("tcpNoDelay", true);
      val b = new Bootstrap()
        .group(workerGroup)
        .channel(classOf[NioSocketChannel])
        .option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
        .handler(new ChannelInitializer[SocketChannel]() {
          override def initChannel(ch: SocketChannel): Unit =
            ch.pipeline
              .addLast(new StringEncoder)
              .addLast(new DelimiterBasedFrameDecoder(1000000, // todo: size?
                true, true, copiedBuffer("\r\n", US_ASCII)))
              .addLast(new StringDecoder)
              .addLast(clientHandler)
        })
      val f = b.connect(host, port).sync
      f.channel.closeFuture.sync
    } catch {
      case ex: InterruptedException => ex.printStackTrace()
    } finally workerGroup.shutdownGracefully()
  }

  def writeMessage(msg: String): ChannelFuture = clientHandler.sendMessage(msg)
}
