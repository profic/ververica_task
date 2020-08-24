package task

import scala.language.implicitConversions

object Ops {
  @inline def repeat(times: Int)(f: => Unit): Unit = 1 to times foreach { _ => f }

}
