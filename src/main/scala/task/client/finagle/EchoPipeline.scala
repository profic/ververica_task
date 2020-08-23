package task.client.finagle

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.charset.StandardCharsets.US_ASCII

import com.twitter.finagle._
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.client.{EndpointerStackClient, StackClient, StdStackClient, Transporter}
import com.twitter.finagle.dispatch.SerialClientDispatcher
import com.twitter.finagle.netty4.Netty4Transporter
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.transport.{Transport, TransportContext}
import com.twitter.util.Await.{ready, result}
import com.twitter.util.{Await, Duration, Future, NullMonitor}
import io.netty.buffer.Unpooled.copiedBuffer
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import task.Constants.{Get, Put, QUIT, SHUTDOWN}

import scala.concurrent.duration.FiniteDuration

class FinagleBaseTopLevelClient(client: Service[String, String]) {

  private val timeout = Duration.fromSeconds(5) // todo

  def writeRead(str: String): String = result(client(s"$str\n") /*, timeout*/)
  def quit(): String = result(client(QUIT) /*, timeout*/)
  def shutdown(): String = result(client(SHUTDOWN) /*, timeout*/)
  def put(s: String): String = result(client(s"$Put$s\n") /*, timeout*/)
  def get(n: Int): String = result(client(s"$Get$n\n") /*, timeout*/)
  def close(): Unit = ready(client.close() /*, timeout*/)
  def getAsync(n: Int): Future[String] = client(s"$Get$n\n")
}

object Echo extends Client[String, String] {

  def main(args: Array[String]): Unit = {

    val k = new FinagleBaseTopLevelClient(
      ClientBuilder()
        .stack(Client())
        .noFailureAccrual
        .monitor(_ => NullMonitor)
        .hosts("localhost:10042")
        .keepAlive(true)
        .tracer(NullTracer)
        .hostConnectionLimit(20)
        .connectTimeout(Duration.fromSeconds(1)) // max time to spend establishing a TCP connection.
        .reportTo(NullStatsReceiver) // export host-level load data to the loaded-StatsReceiver
        .build()
    )

    //    val cl  = newClient("localhost:10042").toService
    val res = k.get(1)
    println(s"res = $res")
  }

  object EchoPipeline extends (ChannelPipeline => Unit) {

    def apply(channelPipeline: ChannelPipeline): Unit = channelPipeline
      .addLast(new StringEncoder())
      .addLast(
        new DelimiterBasedFrameDecoder(1000000, true, true, copiedBuffer("\r\n", US_ASCII)), // todo: size?
        new StringDecoder()
      )
  }

  object Clients {
    val res = new StackBuilder[ServiceFactory[String, String]](com.twitter.finagle.stack.nilStack).result
  }

  //  case class Client(
  //    //          override val stack: Stack[ServiceFactory[String, String]] = Clients.res, // todo: replace with Buffers?
  //    override val stack: Stack[ServiceFactory[String, String]] = StackClient.newStack, // todo: replace with Buffers?
  //    //      override val stack: Stack[ServiceFactory[String, String]] = Stack.Params.empty, // todo: replace with Buffers?
  //    override val params: Stack.Params = Stack.Params.empty
  //  ) extends EndpointerStackClient[String, String, Client] {
  //    override protected def copy1(s: Stack[ServiceFactory[String, String]], p: Stack.Params): Client = copy(s, p)
  //    override protected def endpointer: Stackable[ServiceFactory[String, String]] = ???
  //  }
  //  import scala.concurrent.duration._

  case class Client(
    //          override val stack: Stack[ServiceFactory[String, String]] = Clients.res, // todo: replace with Buffers?
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