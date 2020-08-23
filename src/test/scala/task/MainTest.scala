package task

import java.nio.file.Files

import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import org.apache.commons.lang3.RandomStringUtils.{randomAlphabetic, randomAlphanumeric}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import task.Constants._
import task.Tests._
import task.client.finagle.{TcpClient, FinagleBaseTopLevelClient}
import task.netty.NettyServerScala

import scala.language.implicitConversions

object Tests {
  val InvalidReq: String = removeSeparators(Constants.InvalidReq)
  val ErrorReq  : String = removeSeparators(Constants.Error)
  val Ok        : String = removeSeparators(Constants.Ok)

  private def removeSeparators(s: String) = s.replace("\r\n", "")
}

class MainTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  before(shiftToEnd())

  private val port   = 10042
  private val client = new FinagleBaseTopLevelClient(TcpClient.newClient(s"localhost:$port").toService)

  "asdasd0" should "TABLE" in {
    val blabla = randomAlphabetic(5)

    val t: TableFor2[() => String, String] = Table(
      ("func", "expected last response"),
      (putWithSeqNumber(blabla).get(1), concat(blabla)),
      (putWithSeqNumber(blabla).putWithSeqNumber(blabla).get(1).get(1), concat(blabla)),
      (putWithSeqNumber(blabla, 2).get(2), concat(blabla, 2)),
      (putWithSeqNumber(blabla, 100).get(100), concat(blabla, 100)),
      (putWithSeqNumber(blabla, 100).get(50), concat(blabla, 50)),
    )

    testTable(t)
  }

  "very long word" should "work" in {
    val longSingleWord = randomAlphanumeric(100000)
    putAndCheckOk(longSingleWord).get(1) shouldBe s"$longSingleWord\n"
  }

  "very long line" should "work" in {
    val longLine = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")
    putAndCheckOk(longLine).get(1) shouldBe s"$longLine\n"
  }

  "multiple very long lines returned in multiple requests" should "work" in {
    val longLine1 = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")
    val longLine2 = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")
    val longLine3 = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")
    putAndCheckOk(longLine1).putAndCheckOk(longLine2).putAndCheckOk(longLine3)
    get(1) shouldBe s"$longLine1\n"
    get(1) shouldBe s"$longLine2\n"
    get(1) shouldBe s"$longLine3\n"
  }

  "multiple very long lines returned in one request" should "work" in {
    val longLine1 = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")
    val longLine2 = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")
    val longLine3 = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")
    putAndCheckOk(longLine1).putAndCheckOk(longLine2).putAndCheckOk(longLine3)
    get(3) shouldBe List(longLine1, longLine2, longLine3).map(_ + "\n").mkString
  }

  "asdasd1" should "return read value after write" in {
    val blabla = randomAlphabetic(5)
    put(blabla).get(1) shouldBe s"$blabla\n"
  }

  "asdasd2" should "return read value after write (twice)" in {
    val blabla = randomAlphabetic(5)

    putAndCheckOk(blabla).get(1) shouldBe s"$blabla\n"
    putAndCheckOk(blabla).get(1) shouldBe s"$blabla\n"
  }

  "asdasd3" should "return return ERR if read more one more value after write-read" in {
    put(randomAlphabetic(5)).get(1).get(1) shouldBe ErrorReq
  }

  "asdasd34" should "return return ERR if read more values" in {
    put(randomAlphabetic(5)).get(2) shouldBe ErrorReq
  }

  "asdasd4" should "return values in the same order on multiple reads" in {
    (1 to 10).map(_.toString).foreach(client.put)

    def test(i1: Int, i2: Int) = get(2) shouldBe (i1 to i2).map(s => s"$s\n").mkString

    test(1, 2)
    test(3, 4)
    test(5, 6)
    test(7, 8)
    test(9, 10)
  }

  "asdasd7" should "TABLE 22" in {

    val t: TableFor2[() => String, String] = Table(
      ("func", "expected"),
      (get(0), InvalidReq),
      (get(-1), InvalidReq),
      (client.writeRead("GET bla"), InvalidReq),
      (put(""), InvalidReq),
      (put("?"), InvalidReq),
      (put("invalid?string"), InvalidReq),
      (put("valid string and invalid?string"), InvalidReq),
    )

    testTable(t)
  }

  private def testTable(t: TableFor2[() => String, String]): Unit = t.forEvery { (func, expected) =>
    func() shouldBe expected
    shiftToEnd()
  }

  private def shiftToEnd(): Unit = /* synchronized */ { // todo: synchronized
    //    consumer.toEnd
  }

  private def concat(s: String, n: Int = 1) = (1 to n).map(i => s"$i$s\n").mkString

  private def quit() = client.quit()
  private def get(n: Int) = client.get(n)
  private def put(s: String, n: Int = 1) = (1 to n).map(_ => client.put(s)).last
  private def putWithSeqNumber(s: String, n: Int = 1) = (1 to n).map(_ + s).map(client.put).last

  private def putAndCheckOk(s: String, n: Int = 1) = (1 to n).map { _ =>
    val res = client.put(s)
    res shouldBe Ok
    res
  }.last

  implicit class SOps(ignore: String) {
    def get(n: Int): String = MainTest.this.get(n)
    def putWithSeqNumber(s: String, n: Int = 1): String = MainTest.this.putWithSeqNumber(s, n)
    def quit(): String = MainTest.this.quit()
    def putAndCheckOk(s: String, n: Int = 1): String = MainTest.this.putAndCheckOk(s, n)
  }

  private implicit def futureToFunc(f: => String): () => String = () => f

  override protected def beforeAll(): Unit = {
    val path = Files.createTempDirectory("tmp").toFile
    path.deleteOnExit()

    val queue = ChronicleQueue
      .singleBuilder(path)
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY)
      .build()
    NettyServerScala.newServer(port, queue)
  }

  override protected def afterAll(): Unit = {
    //    Await.ready(server.close(), 5.seconds)
  }
}
