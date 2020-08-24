package task

import java.io.{Closeable, File}
import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils.{randomAlphabetic, randomAlphanumeric}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import task.client.{FinagleBaseTopLevelClient, TcpClient}
import task.server.NettyServerScala
import task.store.Queue

import scala.language.implicitConversions

class IntegrationTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  before(shiftToEnd())

  private val port   = 10042
  private val client = new FinagleBaseTopLevelClient(TcpClient.newClient(s"localhost:$port").toService)

  "it" should "return correct last read value" in {
    val str = randomAlphabetic(5)

    val t: TableFor2[() => String, String] = Table(
      ("func", "expected last response"),
      (putWithSeqNumber(str).get(1), concat(str)),
      (putWithSeqNumber(str).putWithSeqNumber(str).get(1).get(1), concat(str)),
      (putWithSeqNumber(str, 2).get(2), concat(str, 2)),
      (putWithSeqNumber(str, 100).get(100), concat(str, 100)),
      (putWithSeqNumber(str, 100).get(50), concat(str, 50)),
    )

    testTable(t)
  }

  "it" should "correctly write and read very long word" in {
    val longSingleWord = randomAlphanumeric(100000)
    putAndCheckOk(longSingleWord).get(1) shouldBe longSingleWord.addNewLine
  }

  "it" should "correctly write and read very long line" in {
    val longLine = randomWords
    putAndCheckOk(longLine).get(1) shouldBe longLine.addNewLine
  }

  "it" should "correctly write and read multiple very long lines returned in multiple requests" in {
    val longLine1 = randomWords
    val longLine2 = randomWords
    val longLine3 = randomWords

    putAndCheckOk(longLine1).putAndCheckOk(longLine2).putAndCheckOk(longLine3)

    get(1) shouldBe longLine1.addNewLine
    get(1) shouldBe longLine2.addNewLine
    get(1) shouldBe longLine3.addNewLine
  }

  "it" should "correctly write and read multiple very long lines returned in one request" in {
    val longLine1 = randomWords
    val longLine2 = randomWords
    val longLine3 = randomWords

    putAndCheckOk(longLine1).putAndCheckOk(longLine2).putAndCheckOk(longLine3)

    get(3) shouldBe List(longLine1, longLine2, longLine3).map(_.addNewLine).mkString
  }

  "it" should "read value after write" in {
    val str = randomAlphabetic(5)
    put(str).get(1) shouldBe str.addNewLine
  }

  "it" should "read value after write (twice)" in {
    val str = randomAlphabetic(5)

    putAndCheckOk(str).get(1) shouldBe str.addNewLine
    putAndCheckOk(str).get(1) shouldBe str.addNewLine
  }

  "it" should "return return ERR if read more one more value after write-read" in {
    put(randomAlphabetic(5)).get(1).get(1) shouldBe ErrorReq
  }

  "it" should "return return ERR if read more values than exists" in {
    put(randomAlphabetic(5)).get(2) shouldBe ErrorReq
  }

  "it" should "return values in the same order on multiple reads" in {
    (1 to 10).map(_.toString).foreach(client.put)

    def test(i1: Int, i2: Int) = get(2) shouldBe (i1 to i2).map(s => s"$s".addNewLine).mkString

    test(1, 2)
    test(3, 4)
    test(5, 6)
    test(7, 8)
    test(9, 10)
  }

  "it" should "return InvalidReq" in {

    val t: TableFor2[() => String, String] = Table(
      ("func", "expected"),
      (get(0), InvalidReq),
      (get(-1), InvalidReq),
      (client.writeAndRead("GET bla"), InvalidReq),
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
    def get(n: Int): String = IntegrationTest.this.get(n)
    def putWithSeqNumber(s: String, n: Int = 1): String = IntegrationTest.this.putWithSeqNumber(s, n)
    def quit(): String = IntegrationTest.this.quit()
    def putAndCheckOk(s: String, n: Int = 1): String = IntegrationTest.this.putAndCheckOk(s, n)
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

    queue = Queue(tmpDir.getAbsolutePath)
    server = NettyServerScala.newServer(port, queue)
  }

  override protected def afterAll(): Unit = {
    server.close()
    queue.close()
    FileUtils.deleteDirectory(tmpDir)
  }

  val InvalidReq: String = removeLinebreaks(Constants.InvalidReq)
  val ErrorReq  : String = removeLinebreaks(Constants.Error)
  val Ok        : String = removeLinebreaks(Constants.Ok)

  private def removeLinebreaks(s: String) = s.replace("\r\n", "")
}
