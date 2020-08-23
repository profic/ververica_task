package task.client.netty.jaava;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

public class ClientHandler2 extends SimpleChannelInboundHandler<String> {
    ChannelHandlerContext ctx;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx; // todo
    }

    public void sendMessage(String msgToSend) throws InterruptedException {
        if (ctx != null) {
            ctx.writeAndFlush(Unpooled.copiedBuffer(msgToSend, CharsetUtil.UTF_8));
//            if (!cf.sync().isSuccess()) {
//                System.out.println("Send failed: " + cf.cause());
//            }
        } else {
            //ctx not initialized yet. you were too fast. do something here
            System.err.println("CTX IS NULL");
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        this.ctx = ctx; // todo
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext arg0, String msg) throws Exception {
        System.out.println("msg = " + msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    }
}