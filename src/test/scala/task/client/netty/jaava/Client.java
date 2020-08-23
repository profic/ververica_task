package task.client.netty.jaava;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class Client implements Runnable {

    String host = "localhost";
    int    port = 10042;
    private final ClientHandler2  clientHandler = new ClientHandler2();
    private       boolean         isRunning     = false;
    private       ExecutorService executor      = null;


    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.startClient();
        Thread.sleep(2000);
        client.writeMessage("GET 3\r\n");
//        Thread.sleep(1000);
//        client.writeMessage("GET 1\r\n");
//        Thread.sleep(1000);
//        client.writeMessage("GET 1\r\n");
//        Thread.sleep(1000);
//        client.writeMessage("GET 1\r\n");
//        Thread.sleep(1000);
//        client.writeMessage("GET 1\r\n");
//        Thread.sleep(1000);
//        client.writeMessage("GET 1\r\n");
//        Thread.sleep(1000);
//        client.writeMessage("GET 1\r\n");
//        client.stopClient();  //call this at some point to shutdown the client
    }

    public synchronized void startClient() {
        if (!isRunning) {
            executor = Executors.newFixedThreadPool(1);
            executor.execute(this);
            isRunning = true;
        }
    }

    public synchronized boolean stopClient() {
        boolean bReturn = true;
        if (isRunning) {
            if (executor != null) {
                executor.shutdown();
                try {
                    executor.shutdownNow();
                    if (executor.awaitTermination(calcTime(10, 0.66667), TimeUnit.SECONDS)) {
                        if (!executor.awaitTermination(calcTime(10, 0.33334), TimeUnit.SECONDS)) {
                            bReturn = false;
                        }
                    }
                } catch (InterruptedException ie) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                } finally {
                    executor = null;
                }
            }
            isRunning = false;
        }
        return bReturn;
    }

    private long calcTime(int nTime, double dValue) {
        return (long) ((double) nTime * dValue);
    }

    @Override
    public void run() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
//            bootstrap.setOption("sendBufferSize", 1048576);
//            bootstrap.setOption("receiveBufferSize", 1048576);
//            bootstrap.setOption("writeBufferHighWaterMark", 10 * 64 * 1024);

            /*
            I am not sure if "tcpNoDelay" helps to improve the throughput. Delay is there to improve the performance.
            None the less, I tried it and saw that the throughput actually fell more than 90%.
             */
//            bootstrap.setOption("tcpNoDelay", true);


            Bootstrap b = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline()
//                            .addLast(new LoggingHandler())// todo
                              .addLast(new StringEncoder())
                              .addLast(new DelimiterBasedFrameDecoder(
                                      1000000, // todo: size?
                                      true,
                                      true,
                                      copiedBuffer("\r\n", US_ASCII)))
                              .addLast(new StringDecoder())
                              .addLast(clientHandler)
                            ;

                        }
                    });

            ChannelFuture f = b.connect(host, port).sync();

            f.channel().closeFuture().sync();
        } catch (InterruptedException ex) {
            // do nothing
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void writeMessage(String msg) throws InterruptedException {
        clientHandler.sendMessage(msg);
    }
}