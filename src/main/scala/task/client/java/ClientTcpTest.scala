package task.client.java

import java.net.Socket
import java.nio.charset.StandardCharsets.US_ASCII

import scala.annotation.tailrec

import com.twitter.util.TimeoutException
import task.C._

object ClientTcpTest {
  def main(args: Array[String]): Unit = {
    val c = ClientTcpTest("localhost", 10042)
    val putRes = c.put("blabla")
    val getRes = c.get(1)
    println(s"c.put = $putRes")
    println(s"c.get(1) = $getRes")
  }
}

case class ClientTcpTest(host: String, port: Int) {
  private val s = new Socket(host, port)
  private val (in, out) = (s.getInputStream, s.getOutputStream)

  def quit(): String = writeRead(QUIT)

  def shutdown(): String = writeRead(SHUTDOWN)

  def put(s: String): String = writeRead(s"$Put$s")

  def get(n: Int): String = writeRead(s"$Get$n")

  def close(): Unit = s.close()

  def writeRead(s: String): String = {
    write(s)
    val sb = new StringBuilder

    waitDataAvailable()

    @tailrec
    def read(): Unit = {
      if (in.available() > 0) {
        val data = in.read
        if (data != -1) {
          sb.append(data.toChar)
        }
        read()
      }
    }

    read()
    sb.toString()
  }

  private def write(s: String): Unit = {
    out.write(s.getBytes(US_ASCII))
    out.flush()
  }

  @tailrec
  private def waitDataAvailable(waitedCnt: Int = 0): Unit = {
    if (in.available() == 0 && waitedCnt < 500) {
      Thread.sleep(1)
      waitDataAvailable(waitedCnt + 1)
    } else if (in.available() == 0) {
      throw new TimeoutException(s"TIMEOUT")
    }
  }
}