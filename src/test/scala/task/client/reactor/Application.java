package task.client.reactor;

import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

import java.util.concurrent.TimeUnit;

public class Application {

    public static void main(String[] args) {
        Connection connection =
                TcpClient.create()
                         .host("example.com")
                         .port(80)
                         .handle((inbound, outbound) -> outbound.sendString(Mono.just("hello")))
                         .connectNow();

        connection.onDispose().block();

        {
            TcpClient.create()
                     .host("localhost")
                     .port(10042)
                     .doOnConnected(conn -> conn
                             .addHandler(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                     )
                     .connectNow()
                     .onDispose()
                     .block();
        }

    }
}