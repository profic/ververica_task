package task

import java.net.SocketAddress

import scala.util.Random

import com.twitter.finagle.client.{StackClient, StdStackClient, Transporter}
import com.twitter.finagle.dispatch.SerialClientDispatcher
import com.twitter.finagle.netty4.Netty4Transporter
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.transport.{Transport, TransportContext}
import com.twitter.finagle._
import com.twitter.util.Await
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import com.twitter.logging.Logger

object EchoPipeline extends (ChannelPipeline => Unit) {
  def apply(channelPipeline: ChannelPipeline): Unit = channelPipeline
    .addLast(new StringEncoder())
    .addLast(new StringDecoder())
}

object Echo extends Client[String, String] {

  private[this] val log = Logger.get()

  def main(args: Array[String]): Unit = {
    val s = Echo.newService("localhost:8080")
    while(true) {
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

  private case class Client(
      override val stack: Stack[ServiceFactory[String, String]] = StackClient.newStack, // todo: replace with Buffers?
      override val params: Stack.Params = StackClient.defaultParams
  ) extends StdStackClient[String, String, Client] {

    override protected type In = String
    override protected type Out = String
    override protected type Context = TransportContext

    protected def copy1(s: Stack[ServiceFactory[String, String]], p: Stack.Params): Client = copy(s, p)

    override protected def newTransporter(addr: SocketAddress): Transporter[String, String, TransportContext] = {
      Netty4Transporter.raw[String, String](EchoPipeline, addr, StackClient.defaultParams)
    }

    override protected def newDispatcher(transport: Transport[In, Out] {
      type Context <: Client.this.Context
    }): Service[String, String] = new SerialClientDispatcher(transport, NullStatsReceiver)

  }

  override def newService(dest: Name, label: String): Service[String, String] = Client().newService(dest, label)

  override def newClient(dest: Name, label: String): ServiceFactory[String, String] = Client().newClient(dest, label)
}