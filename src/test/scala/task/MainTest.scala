package task

import org.scalactic.source.Position
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MainTest extends AnyFlatSpec with Matchers with BeforeAndAfter {



  "asd" should "asd" in {
    assert(true)
  }

  override protected def before(fun: => Any)(implicit pos: Position): Unit = {
    super.before(fun)
  }

  override protected def after(fun: => Any)(implicit pos: Position): Unit = {
    super.after(fun)
  }
}
