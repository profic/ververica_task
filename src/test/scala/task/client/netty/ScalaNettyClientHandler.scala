package task.client.netty

import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.US_ASCII

import io.netty.buffer.Unpooled
import io.netty.buffer.Unpooled.copiedBuffer
import io.netty.channel.{ChannelFuture, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.util.CharsetUtil

import scala.concurrent.{Future, Promise}


class ScalaNettyClientHandler(ready: Promise[Unit]) extends SimpleChannelInboundHandler[String] {
  private var ctx: ChannelHandlerContext = _

  def sendMessage(msgToSend: String): ChannelFuture = ctx.writeAndFlush(copiedBuffer(msgToSend, US_ASCII))

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    this.ctx = ctx // todo
    ready.completeWith(Future.unit)
//    super.channelActive(ctx) // todo
  }

  override protected def channelRead0(arg0: ChannelHandlerContext, msg: String): Unit = {
//    Thread.sleep(1000)
    println("msg = " + msg)
  }
}