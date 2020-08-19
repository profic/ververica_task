package task.client.finagle

import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.US_ASCII

import scala.util.Random

import com.twitter.finagle._
import com.twitter.finagle.client.{StackClient, StdStackClient, Transporter}
import com.twitter.finagle.dispatch.SerialClientDispatcher
import com.twitter.finagle.netty4.Netty4Transporter
import com.twitter.finagle.stack.nilStack
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.transport.{Transport, TransportContext}
import com.twitter.logging.Logger
import com.twitter.util.Await.{ready, result}
import com.twitter.util.{Await, Duration, Future}
import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelHandlerContext, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.{DelimiterBasedFrameDecoder, LineBasedFrameDecoder}
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import task.C
import task.C.{Get, Put, QUIT, SHUTDOWN}

object EchoPipeline extends (ChannelPipeline => Unit) {

  val tmp = new SimpleChannelInboundHandler[String] {
    override def channelRead0(ctx: ChannelHandlerContext, msg: String): Unit = {
      println(s"msg = $msg")
    }
  }

  def apply(channelPipeline: ChannelPipeline): Unit = channelPipeline
    .addLast(new StringEncoder())
    //    .addLast(new LineBasedFrameDecoder(1000000, true, true), new StringDecoder()) // todo: size?
    .addLast(
      new DelimiterBasedFrameDecoder(1000000, true, true, Unpooled.copiedBuffer("\r\n", US_ASCII)),
      new StringDecoder()
    ) // todo: size?
  //    .addLast(tmp)
}

class TopLevelClient(client: Service[String, String]) {

  private val timeout = Duration.fromSeconds(5) // todo
//  private val timeout = Duration.fromSeconds(500)

  def writeRead(str: String): String = result(client(s"$str\n"), timeout)

  def quit(): String = result(client(QUIT), timeout)

  def shutdown(): String = result(client(SHUTDOWN), timeout)

  def put(s: String): String = result(client(s"$Put$s\n"), timeout)

  def get(n: Int): String = result(client(s"$Get$n\n"), timeout)

  //  def get(n: Int): String = result({
  //    val sb = new StringBuilder
  //    val f1 = client(s"$Get$n\n").map { str =>
  //      sb.append(str)
  //      str
  //    }
  //
  //    (1 until n).foldLeft(f1) { case (z, _) =>
  //      z.flatMap { res =>
  //        sb.append(res)
  //        client("")
  //      }
  //    }.map { _ =>
  //      sb.toString
  //    }
  //  }, timeout)

  //  def quit(): Future[String] = client(QUIT)
  //
  //  def shutdown(): Future[String] = client(SHUTDOWN)
  //
  //  def put(s: String): Future[String] = client(s"$Put$s")
  //
  //  def get(n: Int): Future[String] = client(s"$Get$n")

  def close(): Unit = ready(client.close(), timeout)
}

object Echo extends Client[String, String] {

  private[this] val log = Logger.get()

  def main(args: Array[String]): Unit = {
    val s = Echo.newService("localhost:10042")
    while (true) {
      Await.ready(s("this is echo")
        .onFailure(e =>
          e.printStackTrace()
        )
        .onSuccess(ans =>
          log.info(s"ans = ${ans}")
        ))
      Thread.sleep(1000)
      if (Random.nextInt(11) == 10) {
        Await.ready(s("QUIT"))
      }
    }
  }

  object Clients {
    val res = new StackBuilder[ServiceFactory[String, String]](com.twitter.finagle.stack.nilStack).result
  }

  case class Client(
//      override val stack: Stack[ServiceFactory[String, String]] = Clients.res, // todo: replace with Buffers?
            override val stack: Stack[ServiceFactory[String, String]] = StackClient.newStack, // todo: replace with Buffers?
//      override val stack: Stack[ServiceFactory[String, String]] = Stack.Params.empty, // todo: replace with Buffers?
      override val params: Stack.Params = Stack.Params.empty
  ) extends StdStackClient[String, String, Client] {

    override protected type In = String
    override protected type Out = String
    override protected type Context = TransportContext

    protected def copy1(s: Stack[ServiceFactory[String, String]], p: Stack.Params): Client = copy(s, p)

    override protected def newTransporter(addr: SocketAddress): Transporter[String, String, TransportContext] = {
      //      Netty4Transporter.raw[String, String](EchoPipeline, addr, StackClient.defaultParams)
      Netty4Transporter.raw[String, String](EchoPipeline, addr, {
//        (Transport.Options(noDelay = true, reuseAddr = true, reusePort = true), Transport.Options.param)

        Stack.Params.empty
      })
    }

    override protected def newDispatcher(transport: Transport[In, Out] {
      type Context <: Client.this.Context
    }): Service[String, String] = new SerialClientDispatcher(transport, NullStatsReceiver)

  }

  override def newService(dest: Name, label: String): Service[String, String] = Client().newService(dest, label)

  override def newClient(dest: Name, label: String): ServiceFactory[String, String] = Client().newClient(dest, label)
}