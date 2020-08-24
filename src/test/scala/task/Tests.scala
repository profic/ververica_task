package task

object Tests {
  val InvalidReq: String = removeLinebreaks(Constants.InvalidReq)
  val ErrorReq  : String = removeLinebreaks(Constants.Error)
  val Ok        : String = removeLinebreaks(Constants.Ok)

  private def removeLinebreaks(s: String) = s.replace("\r\n", "")
}
