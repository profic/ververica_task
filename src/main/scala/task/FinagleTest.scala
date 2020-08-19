//package task
//
//import java.net.InetSocketAddress
//
//import com.twitter.finagle.{Http, Service, ServiceFactory, Stack}
//import com.twitter.finagle.builder.ServerBuilder
//import com.twitter.finagle.dispatch.SerialServerDispatcher
//import com.twitter.finagle.netty4.Netty4Listener
//import com.twitter.finagle.server.{Listener, StackServer, StdStackServer}
//import com.twitter.finagle.tracing.NullTracer
//import com.twitter.finagle.transport.Transport
//import com.twitter.util.Future
//import io.netty.handler.codec.{DelimiterBasedFrameDecoder, Delimiters}
//
//object FinagleTest {
//
//
//}
//
//case class Server(
//    stack: Stack[ServiceFactory[String, String]] = StackServer.newStack,
//    params: Stack.Params = StackServer.defaultParams)
//  extends StdStackServer[String, String, Server] {
//  protected type In = String
//  protected type Out = String
//
//  protected def copy1(
//      stack: Stack[ServiceFactory[String, String]] = this.stack,
//      params: Stack.Params = this.params
//  ): Server = copy(stack, params)
//
//  protected def newListener(): Listener[String, String] =
//    Netty4Listener(StringServerPipeline, params)
//
//  protected def newDispatcher(
//      transport: Transport[String, String],
//      service: Service[String, String]
//  ) =
//    new SerialServerDispatcher(transport, service)
//}
//
//import io.netty.handler.codec.string.{StringEncoder, StringDecoder}
//import io.netty.channel._
//import io.netty.channel.ChannelPipeline
//import io.netty.util.CharsetUtil
//
//object StringServerPipeline extends ChannelPipelineFactory {
//  def getPipeline = {
//    val pipeline = Channels.pipeline()
//    pipeline.addLast("line", new DelimiterBasedFrameDecoder(100, Delimiters.lineDelimiter: _*))
//    pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.US_ASCII))
//    pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.US_ASCII))
//    pipeline
//  }
//}