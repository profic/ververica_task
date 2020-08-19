package task.client.finagle

import java.net.SocketAddress

import com.twitter.finagle._
import com.twitter.finagle.client.{StackClient, StdStackClient, Transporter}
import com.twitter.finagle.dispatch.SerialClientDispatcher
import com.twitter.finagle.netty4.Netty4Transporter
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.finagle.transport.{Transport, TransportContext}
import com.twitter.util.Await.result
import com.twitter.util.{Await, Future, StorageUnit}
import io.netty.buffer.{ByteBuf, ByteBufUtil}
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringEncoder
import task.C._

object ClientTcpByteBuf extends (ChannelPipeline => Unit) {
  def apply(channelPipeline: ChannelPipeline): Unit = channelPipeline
//    .addLast(new StringEncoder())
//    .addLast(new LineBasedFrameDecoder(1000000), new StringEncoder()) // todo: size?
  //    .addLast(new MessageToMessageEncoder[String] {
  //      override def encode(ctx: ChannelHandlerContext, msg: String, out: util.List[AnyRef]): Unit = {
  //        while (ctx.channel().read().isR)
  //      }
  //    })
}

class TopLevelClientByteBuf(client: Service[String, ByteBuf]) {

  def quit(): String = result(client(QUIT).map(_.toString))

  def shutdown(): String = result(client(SHUTDOWN).map(_.toString))

  def put(s: String): String = result(client(s"$Put$s\n").map { buf =>
    new String(ByteBufUtil.getBytes(buf))
  })

  def get(n: Int): String = result(client(s"$Get$n\n").map { buf =>
    new String(ByteBufUtil.getBytes(buf))
  })

  //  def quit(): Future[String] = client(QUIT).map(_.toString)
  //
  //  def shutdown(): Future[String] = client(SHUTDOWN).map(_.toString)
  //
  //  def put(s: String): Future[String] = client(s"$Put$s\n").map { buf =>
  //    new String(ByteBufUtil.getBytes(buf))
  //  }
  //
  //  def get(n: Int): Future[String] = client(s"$Get$n\n").map { buf =>
  //    new String(ByteBufUtil.getBytes(buf))
  //  }
}

object ClientTcpByteBufEcho extends Client[String, ByteBuf] {

  case class Client(
      override val stack: Stack[ServiceFactory[String, ByteBuf]] = StackClient.newStack, // todo: replace with Buffers?
      override val params: Stack.Params = StackClient.defaultParams
  ) extends StdStackClient[String, ByteBuf, Client] {

    override protected type In = String
    override protected type Out = ByteBuf
    override protected type Context = TransportContext

    protected def copy1(s: Stack[ServiceFactory[String, ByteBuf]], p: Stack.Params): Client = copy(s, p)

    override protected def newTransporter(addr: SocketAddress): Transporter[String, ByteBuf, TransportContext] = {
      Netty4Transporter.raw[String, ByteBuf](ClientTcpByteBuf, addr, StackClient.defaultParams)
    }

    override protected def newDispatcher(transport: Transport[In, Out] {
      type Context <: Client.this.Context
    }): Service[String, ByteBuf] = new SerialClientDispatcher(transport, NullStatsReceiver)

  }

  override def newService(dest: Name, label: String): Service[String, ByteBuf] = Client().newService(dest, label)

  override def newClient(dest: Name, label: String): ServiceFactory[String, ByteBuf] = {
    //    Client().newClient(dest, label)
    Client()
      .withStatsReceiver(NullStatsReceiver)
      .withTracer(NullTracer)
      .withTransport.receiveBufferSize(StorageUnit.fromMegabytes(10)) // todo: size?
      .newClient(dest, label)
  }
}