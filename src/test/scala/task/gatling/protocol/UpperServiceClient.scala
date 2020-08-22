package task.gatling.protocol

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Tcp}
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import task.client.finagle.{Echo, TopLevelClient}

object UpperServiceClient {

  import concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    val c1 = new TopLevelClient(Echo.newClient(s"localhost:10042").toService)
    val c2 = new TopLevelClient(Echo.newClient(s"localhost:10042").toService)
    val c3 = new TopLevelClient(Echo.newClient(s"localhost:10042").toService)
    val c4 = new TopLevelClient(Echo.newClient(s"localhost:10042").toService)

    {
      val res = c1.get(1)
      println(s"res = ${res}")
    }

    {
      val res = c2.get(1)
      println(s"res = ${res}")
    }

//    sys.exit(1)

    List(c1
      , c2, c3, c4
    ).map { c =>
      Future {
        var i = 0
        while (true) {
          try {
            val res = c.get(1)
            i += 1
            if (i % 10000 == 0) {
              println(Thread.currentThread().getName + ", res = " + res)
            }
          } catch {
            case _: Throwable =>
          }
        }
      }
    }.foreach(sd => Await.ready(sd, Duration.Inf))

  }

  val Ok: ByteString = ByteString("")
  val InvalidRequest: ByteString = ByteString("")
}

class UpperServiceClient(ip: String, port: Int) {

  import UpperServiceClient._

  private val c = new TopLevelClient(Echo.newClient(s"localhost:$port").toService)

  def run(): String = {
    c.get(1)
  }

  //  def run(): String = {
  //    implicit val system: ActorSystem = ActorSystem("ClientSys")
  //    implicit val materializer: ActorMaterializer = ActorMaterializer()
  //
  //    val testInput = ('a' to 'z').map(ByteString(_))
  //
  //    val result: Future[ByteString] = Source(testInput)
  //      .via(Tcp().outgoingConnection(ip, port))
  //      .runFold(ByteString.empty)((acc, in) => acc ++ in)
  //
  ////    val res: ByteString = Await.result(result, 10.seconds)
  //    val res = Await.result(result, 10.seconds)
  //    if (res == Ok) ""
  ////    else if
  //    ???
  //  }
}
