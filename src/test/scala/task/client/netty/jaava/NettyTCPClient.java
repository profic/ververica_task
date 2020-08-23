package task.client.netty.jaava;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyTCPClient {

    public static void main(String[] args) {
        NettyTCPClient nettyTCPClient = new NettyTCPClient();
        nettyTCPClient.start("localhost", 10042);
    }

    void start(String host, int port) {
        // Event loop group to Handle I/O operations for channel
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        // Help boot strapping a channel
        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap
                .group(eventLoopGroup) // associate event loop to channel
                .channel(NioSocketChannel.class) // create a NIO socket channel
                .handler(new TCPClientChannelInitializer()); // Add channel initializer

        try {
            // Connect to listening server
            ChannelFuture channelFuture = clientBootstrap.connect(host, port).sync();
            // Check if channel is connected
            if (channelFuture.isSuccess()) {
                System.out.println("connected");
            }
            // Block till channel is connected
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Connection is closed, clean up
            System.out.println("closing");
            eventLoopGroup.shutdownGracefully();
        }
    }
}

class TCPClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        // Configure encoders/decoder or codecs
        socketChannel.pipeline().addLast(new StringDecoder());
        socketChannel.pipeline().addLast(new StringEncoder());
        // Add Custom Inbound handler to handle incoming traffic
        socketChannel.pipeline().addLast(new TCPClientInboundHandler());
    }
}

class TCPClientInboundHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) {
        channelHandlerContext.writeAndFlush("Acknowledged"); // Return a sample response
    }
}