package task

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import task.client.finagle.{Echo, FinagleBaseTopLevelClient}


object NettyServerHandlerScalaTest {

  def main(args: Array[String]): Unit = {
    List(1
      , 2, 3, 4
    ).map { _ =>
      Future {
        val c = new FinagleBaseTopLevelClient(Echo.newClient("localhost:10042").toService)
        var i = 0
        while (true) {
          try {
            val res = c.get(1)

            i += 1
            if (i % 10000 == 0) {
              println(s"${Thread.currentThread().getName}, res = $res")
            }
          } catch {
            case _: Throwable =>
          }
        }
      }
    }.foreach(sd => Await.ready(sd, Duration.Inf))
  }
}
