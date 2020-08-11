package task

trait ReqType

case object PUT extends ReqType
case object GET extends ReqType
case object QUIT extends ReqType
case object SHUTDOWN extends ReqType
case object UNKNOWN extends ReqType