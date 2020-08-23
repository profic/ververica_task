package task.client.netty

import task.Constants._

class NettyBaseTopLevelClient(client: ScalaClientNetty) {

  //  private val timeout = Duration.fromSeconds(5) // todo

  import client.writeMessage

  def writeRead(str: String) = {
    (writeMessage(s"$str\n") /*, timeout*/)
  }
  def quit() = (writeMessage(QUIT) /*, timeout*/)
  def shutdown() = (writeMessage(SHUTDOWN) /*, timeout*/)
  def put(s: String) = (writeMessage(s"$Put$s\n") /*, timeout*/)
  def get(n: Int) = (writeMessage(s"$Get$n\n") /*, timeout*/)
  def close(): Unit = client.stopClient
}