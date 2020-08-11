package task.finagle

import java.net.Socket

import org.apache.commons.io.IOUtils

object ClientTcpTest {

  def main(args: Array[String]): Unit = {
    val s = new Socket("localhost", 8080)
    val out = s.getOutputStream
    out.write("reqreq".getBytes)
    out.flush()

    val is = s.getInputStream
    while (is.available() == 0) {
      println("SLEEP")
      Thread.sleep(1000)
    }




//    val arr = Array.ofDim[Byte](1024 * 1024 * 1024)
//    IOUtils.readFully(s.getInputStream, arr)
//    println(new String(arr))


  }

}
