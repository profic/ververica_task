package task.finagle

import com.twitter.finagle.Stack.Params
import com.twitter.finagle._
import com.twitter.finagle.dispatch.SerialServerDispatcher
import com.twitter.finagle.netty4.Netty4Listener
import com.twitter.finagle.server.{StackServer, StdStackServer}
import com.twitter.finagle.transport.{Transport, TransportContext}
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.LineBasedFrameDecoder

case class Server(
    stack: Stack[ServiceFactory[ByteBuf, ByteBuf]] = StackServer.newStack, // todo: replace with Buffers?
    params: Params = StackServer.defaultParams + param.ProtocolLibrary("tcp")
) extends StdStackServer[ByteBuf, ByteBuf, Server] {

  override protected def copy1(s: Stack[ServiceFactory[ByteBuf, ByteBuf]] = stack, p: Params = params): Server = copy(s, p)

  override protected type In = ByteBuf
  override protected type Out = ByteBuf
  override protected type Context = TransportContext

  override protected def newListener(): Netty4Listener[ByteBuf, ByteBuf, TransportContext] = {
//    Netty4Listener(_ => {}, params) // todo: private[finagle] case class BackPressure(enabled: Boolean) {
    Netty4Listener(_.addLast(new LineBasedFrameDecoder(1000000)), params) // todo: size limit?
  }

  override protected def newDispatcher(
      transport: Transport[In, Out] {type Context <: Server.this.Context},
      service: Service[ByteBuf, ByteBuf]
  ) = new SerialServerDispatcher(transport, service)
}