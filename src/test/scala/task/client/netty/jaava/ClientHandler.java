package task.client.netty.jaava;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private ChannelHandlerContext ctx;

    public static void main(String[] args) throws Exception {


        EventLoopGroup group = new NioEventLoopGroup();
        try {
            new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress("localhost", 10042))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel sc) {
                            sc.pipeline()
                              .addLast(new StringEncoder())
                              .addLast(new DelimiterBasedFrameDecoder(
                                      1000000, // todo: size?
                                      true,
                                      true,
                                      copiedBuffer("\r\n", US_ASCII)))
                              .addLast(new StringDecoder())
                              .addLast(new ClientHandler());
                        }
                    })
                    .connect().sync()
                    .channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }


    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        ctx.writeAndFlush(copiedBuffer("Netty Rocks!", CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println("Client received: " + in.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
