package task.finagle

import java.nio.charset.StandardCharsets

import com.twitter.finagle.Stack.Params
import com.twitter.finagle._
import com.twitter.finagle.dispatch.SerialServerDispatcher
import com.twitter.finagle.netty4.Netty4Listener
import com.twitter.finagle.server.{StackServer, StdStackServer}
import com.twitter.finagle.transport.{Transport, TransportContext}
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}

object StringServer {

  val protocolLibrary = "string"

  private object StringServerPipeline extends (ChannelPipeline => Unit) {
    def apply(pipeline: ChannelPipeline): Unit = {
      pipeline.addLast("stringDecoder", new StringDecoder(StandardCharsets.US_ASCII))
      pipeline.addLast("stringEncoder", new StringEncoder(StandardCharsets.US_ASCII))
    }
  }

  case class Server(
      stack: Stack[ServiceFactory[String, String]] = StackServer.newStack, // todo: replace with Buffers?
      params: Params = StackServer.defaultParams + param.ProtocolLibrary(protocolLibrary)
  ) extends StdStackServer[String, String, Server] {

    override protected def copy1(s: Stack[ServiceFactory[String, String]] = stack, p: Params = params) = copy(s, p)

    override protected type In = String
    override protected type Out = String
    override protected type Context = TransportContext

    override protected def newListener(): Netty4Listener[String, String, TransportContext] = {
      Netty4Listener(StringServerPipeline, params)
    }

    override protected def newDispatcher(
        transport: Transport[In, Out] {type Context <: Server.this.Context},
        service: Service[String, String]
    ) = new SerialServerDispatcher(transport, service)
  }

  def server: Server = Server()
}