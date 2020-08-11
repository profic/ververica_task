package task.load

import com.twitter.logging.Logger
import com.twitter.util.Await
import task.Echo

object LoadTest {

  private[this] val log = Logger.get()


  def main(args: Array[String]): Unit = {
    val s = Echo.newService("localhost:8080")

    // rps       = msgPerProducer * producerCount
    // msg       = randomWord * wordCount
    // wordCount = peekRandom( random(100, 150), random())
    //

    Await.ready(s("this is echo")
      .onFailure(e =>
        log.error(e, "this is error")
      )
      .onSuccess(ans => print(ans)))
  }
}
