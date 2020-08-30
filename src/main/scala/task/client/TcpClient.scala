package task.client

import java.net.SocketAddress
import java.nio.charset.StandardCharsets.US_ASCII

import com.twitter.finagle.Stack.Params
import com.twitter.finagle._
import com.twitter.finagle.client.{StackClient, StdStackClient, Transporter}
import com.twitter.finagle.dispatch.SerialClientDispatcher
import com.twitter.finagle.netty4.Netty4Transporter
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.transport.{Transport, TransportContext}
import com.twitter.util.Await.{ready, result}
import com.twitter.util.Duration
import io.netty.buffer.Unpooled.copiedBuffer
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import task.Constants._
import task.client.TcpClient.Pipeline
import task.server.NettyTcpServer

/**
 * Classes in this file are created for testing purposes, even though they are fully-functional.
 */
class FinagleBaseTopLevelClient(client: Service[String, String]) {

  private val timeout = Duration.fromSeconds(5)

  def writeAndRead(str: String): String = result(client(s"$str\n"), timeout)
  def quit(): String = result(client(Quit), timeout)
  def shutdown(): String = result(client(Shutdown), timeout)
  def put(s: String): String = result(client(s"$Put$s\n"), timeout)
  def get(n: Int): String = result(client(s"$Get$n\n"), timeout)
  def close(): Unit = ready(client.close(), timeout)
}

object TcpClient extends Client[String, String] {

  object Pipeline extends (ChannelPipeline => Unit) {

    def apply(channelPipeline: ChannelPipeline): Unit = {
      val maxLineSize = NettyTcpServer.config.getInt("max-line-size")
      channelPipeline
        .addLast(new StringEncoder())
        .addLast(new DelimiterBasedFrameDecoder(maxLineSize, true, true, copiedBuffer("\r\n", US_ASCII)))
        .addLast(new StringDecoder())

    }
  }

  def apply(): TcpClient = new TcpClient()

  override def newService(dest: Name, label: String): Service[String, String] = TcpClient().newService(dest, label)

  override def newClient(dest: Name, label: String): ServiceFactory[String, String] = TcpClient().newClient(dest, label)
}

class TcpClient(
  override val stack: Stack[ServiceFactory[String, String]] = StackClient.newStack,
  override val params: Params = Params.empty
) extends StdStackClient[String, String, TcpClient] {

  override protected type In = String
  override protected type Out = String
  override protected type Context = TransportContext

  protected def copy1(s: Stack[ServiceFactory[String, String]], p: Params): TcpClient = new TcpClient(s, p)

  override protected def newTransporter(addr: SocketAddress): Transporter[String, String, TransportContext] = {
    Netty4Transporter.raw[String, String](Pipeline, addr, Params.empty)
  }

  override protected def newDispatcher(transport: Transport[In, Out] {
    type Context <: TcpClient.this.Context
  }): Service[String, String] = new SerialClientDispatcher(transport, NullStatsReceiver)
}
