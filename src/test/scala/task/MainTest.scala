package task

import java.io.{Closeable, File}
import java.nio.file.Files

import net.openhft.chronicle.queue.{ChronicleQueue, RollCycles}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils.{randomAlphabetic, randomAlphanumeric}
import org.mockito.Mockito.spy
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import task.Tests._
import task.client.{FinagleBaseTopLevelClient, TcpClient}
import task.server.NettyServerScala
import task.store.Queue

import scala.language.implicitConversions

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
    putAndCheckOk(longSingleWord).get(1) shouldBe longSingleWord.addNewLine
  }

  "very long line" should "work" in {
    val longLine = randomWords
    putAndCheckOk(longLine).get(1) shouldBe longLine.addNewLine
  }

  "multiple very long lines returned in multiple requests" should "work" in {
    val longLine1 = randomWords
    val longLine2 = randomWords
    val longLine3 = randomWords

    putAndCheckOk(longLine1).putAndCheckOk(longLine2).putAndCheckOk(longLine3)

    get(1) shouldBe longLine1.addNewLine
    get(1) shouldBe longLine2.addNewLine
    get(1) shouldBe longLine3.addNewLine
  }

  "multiple very long lines returned in one request" should "work" in {
    val longLine1 = randomWords
    val longLine2 = randomWords
    val longLine3 = randomWords

    putAndCheckOk(longLine1).putAndCheckOk(longLine2).putAndCheckOk(longLine3)

    get(3) shouldBe List(longLine1, longLine2, longLine3).map(_.addNewLine).mkString
  }

  "asdasd1" should "return read value after write" in {
    val blabla = randomAlphabetic(5)
    put(blabla).get(1) shouldBe blabla.addNewLine
  }

  "asdasd2" should "return read value after write (twice)" in {
    val blabla = randomAlphabetic(5)

    putAndCheckOk(blabla).get(1) shouldBe blabla.addNewLine
    putAndCheckOk(blabla).get(1) shouldBe blabla.addNewLine
  }

  "asdasd3" should "return return ERR if read more one more value after write-read" in {
    put(randomAlphabetic(5)).get(1).get(1) shouldBe ErrorReq
  }

  "asdasd34" should "return return ERR if read more values" in {
    put(randomAlphabetic(5)).get(2) shouldBe ErrorReq
  }

  "asdasd4" should "return values in the same order on multiple reads" in {
    (1 to 10).map(_.toString).foreach(client.put)

    def test(i1: Int, i2: Int) = get(2) shouldBe (i1 to i2).map(s => s"$s".addNewLine).mkString

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

  private def shiftToEnd(): Unit = queue.drainAll()

  private def concat(s: String, n: Int = 1) = (1 to n).map(i => s"$i${s.addNewLine}").mkString

  private def quit() = client.quit()
  private def get(n: Int) = client.get(n)
  private def put(s: String, n: Int = 1) = (1 to n).map(_ => client.put(s)).last
  private def putWithSeqNumber(s: String, n: Int = 1) = (1 to n).map(_ + s).map(client.put).last

  private def putAndCheckOk(s: String, n: Int = 1) = (1 to n).map { _ =>
    val res = client.put(s)
    res shouldBe Ok
    res
  }.last

  implicit class SOps(s: String) {
    def get(n: Int): String = MainTest.this.get(n)
    def putWithSeqNumber(s: String, n: Int = 1): String = MainTest.this.putWithSeqNumber(s, n)
    def quit(): String = MainTest.this.quit()
    def putAndCheckOk(s: String, n: Int = 1): String = MainTest.this.putAndCheckOk(s, n)
    def addNewLine: String = s + "\n"
  }

  private implicit def futureToFunc(f: => String): () => String = () => f

  private var server: Closeable = _
  private var queue : Queue     = _
  private var tmpDir: File      = _

  private def randomWords = (1 to 10000).map(_ => randomAlphanumeric(2, 10)).mkString(" ")

  override protected def beforeAll(): Unit = {
    tmpDir = Files.createTempDirectory("tmp").toFile
    //    tmpDir = new File(s"""d:/${RandomStringUtils.randomAlphanumeric(3)}/${RandomStringUtils.randomAlphanumeric(3)}""")

    sys.addShutdownHook(afterAll())

    val q = spy(ChronicleQueue
      .singleBuilder(tmpDir)
      .maxTailers(1)
      .rollCycle(RollCycles.LARGE_HOURLY)
      .build())

    queue = new Queue(q)

    server = NettyServerScala.newServer(port, queue)
  }

  override protected def afterAll(): Unit = {
    server.close()
    queue.close()
    FileUtils.deleteDirectory(tmpDir)
  }
}
