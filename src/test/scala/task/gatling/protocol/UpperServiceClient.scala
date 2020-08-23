package task.gatling.protocol

import akka.util.ByteString
import task.client.finagle.{TcpClient, FinagleBaseTopLevelClient}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

// todo: remove
object UpperServiceClient {

  import concurrent.ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    val c1 = new FinagleBaseTopLevelClient(TcpClient.newClient(s"localhost:10042").toService)
    val c2 = new FinagleBaseTopLevelClient(TcpClient.newClient(s"localhost:10042").toService)
    val c3 = new FinagleBaseTopLevelClient(TcpClient.newClient(s"localhost:10042").toService)
    val c4 = new FinagleBaseTopLevelClient(TcpClient.newClient(s"localhost:10042").toService)

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

  val Ok            : ByteString = ByteString("")
  val InvalidRequest: ByteString = ByteString("")
}
