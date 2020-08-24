package task

object Constants {

  val AllowedChars: Array[Byte] = (Array(' ') ++ ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z')).map(_.toByte)

  val defaultTailerName = "default"

  val Shutdown   = "SHUTDOWN\n"
  val Quit       = "QUIT\n"
  val InvalidReq = "INVALID_REQUEST\r\n"
  val Ok         = "OK\r\n"
  val Error      = "ERR\r\n"
  val Put        = "PUT "
  val Get        = "GET "
}
