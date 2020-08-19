package task.akka

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.file.{Files, Paths}

import akka.io.Tcp.Write
import task.akka.EchoServer.config

class SimplisticHandler extends Actor {

  import Tcp._

  def receive: Receive = {
    case Received(data) => sender() ! Write(data)
    case PeerClosed => context.stop(self)
  }
}

class AkkaTcpServer extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 0))

  def receive: Receive = {
    case b@Bound(localAddress) =>
      context.parent ! b

    case CommandFailed(_: Bind) => context.stop(self)

    case c@Connected(remote, local) =>
      val handler = context.actorOf(Props[SimplisticHandler])
      val connection = sender()
      connection ! Register(handler)
  }

}


object Client {
  def main(args: Array[String]): Unit = {
    val line1 = Files.readAllLines(Paths.get("""c:\Users\Vlad\Desktop\lines.txt""")).get(0)
    implicit val system = ActorSystem("EchoServer", config)
    val aktor = system.actorOf(props(new InetSocketAddress("localhost", 8080)))
    Thread.sleep(2000)
    aktor ! ByteString.fromString(line1)

  }

  def props(remote: InetSocketAddress): Props = Props(new Client(remote))
}

class Client(remote: InetSocketAddress) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive: Receive = {
    case CommandFailed(_: Connect) =>
      println("connect failed")
      context.stop(self)

    case c@Connected(remote, local) =>
      val connection = sender()
      connection ! Register(self)
      context.become {
        case data: ByteString =>
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          println("write failed")
        case Received(data) =>
          //          listener ! data
//          connection ! Write(data)
        //          println(s"Received data: ${data.decodeString(US_ASCII)}")
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          println("connection closed")
          context.stop(self)
      }
  }
}