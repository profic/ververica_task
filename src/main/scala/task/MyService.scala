package task

import java.util.regex.Pattern

import com.twitter.finagle.{ClientConnection, ListeningServer, Service}
import com.twitter.logging.Logger
import com.twitter.util.Future
import net.openhft.chronicle.queue.{ExcerptAppender, ExcerptTailer}

object MyService {
  val log: Logger = Logger.get()
  val p: Pattern = Pattern.compile("[A-Za-z0-9]+")
}

class MyService(
    private val server: ListeningServer,
    private val connection: ClientConnection,
    private val producer: ExcerptAppender,
    private val consumer: ExcerptTailer,
    private val countingTailer: CountingTailer
) extends Service[String, String] {

  import C._

  override def apply(request: String): Future[String] = Future.value {
    if (QUIT == request) quit
    else if (SHUTDOWN == request) shutdown
    else if (request.startsWith("PUT ")) write(request)
    else if (request.startsWith("GET ")) read(request) // todo: replace with Buffers?
    else invalidRequest
  }

  private def shutdown = {
    MyService.log.debug("shutting down server")
    server.close()
    ok
  }

  private def quit = {
    MyService.log.debug("closing connection")
    connection.close()
    ok
  }

  private def write(request: String) = {
    val string = getActualRequest(request)
    if (validate(string)) {
      producer.writeText(string)
      ok
    } else invalidRequest
  }

  private def getActualRequest(request: String) = request.substring(4, request.length)

  private def validate(string: String) = string.split(" ").forall(MyService.p.matcher(_).matches())

  private def read(request: String) = {
    val n = getActualRequest(request).toInt

    val end = countingTailer.end
    if (end != 0 && end - consumer.index >= n) { // todo: is long enough?
      val sb = new StringBuilder
      (1 to n).foreach { _ =>
        val doc = consumer.readingDocument()
        if (doc.isPresent) {
          val text = doc.wire().asText() // todo: replace with Buffers?
          doc.close()
          sb.append(text)
        } else {
          throw new IllegalStateException("") // todo
        }
      }
      sb.toString
    } else {
      Error
    }
  }
}