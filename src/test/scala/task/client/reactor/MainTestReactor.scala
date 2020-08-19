//package task.client.reactor
//
//import scala.language.implicitConversions
//
//import com.twitter.conversions.DurationOps._
//import com.twitter.finagle.{ListeningServer, Service}
//import com.twitter.util.{Await, Future}
//import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue
//import net.openhft.chronicle.queue.{ChronicleQueue, ExcerptTailer, RollCycles}
//import org.apache.commons.lang3.RandomStringUtils
//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
//import task.PutAllTogether.ServOps
//import task.PutAllTogether
//import task.client.finagle.{Echo, TopLevelClient}
//
//
//class MainTestReactor extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfter {
//
//  private var server: ListeningServer = _
//  private var consumer: ExcerptTailer = _
//  //  private var c: TopLevelClientByteBuf = _
//  private var c: TopLevelClient = _
//
//  //  var lowClient: Service[String, ByteBuf] = _
//  var lowClient: Service[String, String] = _
//  private val port = 10042
//
//  private def client = c // todo: need?
//
//  val path = "d:\\ververica_task\\" + RandomStringUtils.randomAlphabetic(3)
//  val queue: SingleChronicleQueue = ChronicleQueue
//    .singleBuilder(path)
//    .maxTailers(1)
//    .rollCycle(RollCycles.LARGE_HOURLY)
//    //      .storeFileListener(listener)
//    .build()
//
//
//  it should "qweqwe" in {
//    val blabla = "blabla\n"
//    val putResult = Await.result(put(blabla, 100))
//    println(s"putResult = $putResult")
//    val res = Await.result(get(50))
//    println(s"res = $res")
//    val expected = concat(blabla, 50)
//    res shouldBe expected
//  }
//
////  TcpClient.create()
////    .host("localhost")
////    .port(4059)
////    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
////    .wiretap(true)
////    .connect()
////    .flatMap(connection =>
////      connection.outbound().sendByteArray(Mono.just(Array[Byte](1, 2, 3)))
////        .then(connection.inbound().receive().asByteArray().next().flatMap(bytes => {
////          println(s"bytes = ${bytes.mkString("Array(", ", ", ")")}")
////          Mono.empty();
////        })).sendByteArray(Mono.just(Array[Byte](5, 6, 7)))
////        .then(connection.inbound().receive().asByteArray().next().flatMap(bytes => {
////          println(s"bytes = ${bytes.mkString("Array(", ", ", ")")}")
////          Mono.empty();
////        }))
////        .then()
////    )
////    .subscribe();
//
//  it should "return values in the same order on multiple reads" in {
//    Await.ready(put1(10))
//
//    println({
//      Await.result(get(10))
//    })
//
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//    println({
//      Await.result(get(2))
//    })
//
//
//    {
//      val res = Await.result(get(2))
//      val expected = concat3(1, 2)
//      res shouldBe expected
//    }
//
//    {
//      val res = Await.result(get(2))
//      val expected = concat3(3, 4)
//      res shouldBe expected
//    }
//
//    {
//      val res = Await.result(get(2))
//      val expected = concat3(5, 6)
//      res shouldBe expected
//    }
//
//    {
//      val res = Await.result(get(2))
//      val expected = concat3(7, 8)
//      res shouldBe expected
//    }
//
//    {
//      val res = Await.result(get(2))
//      val expected = concat3(9, 10)
//      res shouldBe expected
//    }
//
//  }
//
//  private def concat(s: String, n: Int) = (1 to n).mkString
//
//  private def concat2(n: Int) = (1 to n).map(_ + "\n").mkString
//
//  private def concat3(from: Int, toInclusive: Int) = (from to toInclusive).map(_ + "\n").mkString
//
//  private def quit() = client.quit()
//
//  private def put(s: String, n: Int = 1) = {
//    //    (1 to n).foldLeft(Future.value(""))((f, _) => f.flatMap(_ => client.put(s)))
//
//    //    val head :: tail = (1 to n).toList.map(_ => client.put(s))
//    //    tail.foldLeft(head)((f1, f2) => f1.flatMap(_ => f2))
//
//    val _ :: tail = (1 to n).toList
//    val fHead = client.put(s)
//    tail.foldLeft(fHead)((f1, _) => f1.flatMap(_ => {
//      val putRes = client.put(s)
//      println(s"Await.result(putRes) = ${Await.result(putRes)}")
//      putRes
//    }))
//  }
//
//  private def put1(n: Int = 1) = {
//    (1 to n).map(_ + "\n").foreach(f2 => {
//      val putRes = Await.result(client.put(f2))
//      println(s"putRes = $putRes")
//    })
//    Future.Done
//  }
//
//  //  private def put1(n: Int = 1) = {
//  //    val head :: tail = (1 to n).map(_ + "\n").toList
//  //    val fHead = client.put(head)
//  //    tail.foldLeft(fHead)((f1, f2) => f1.flatMap(_ => client.put(f2 + "")))
//  //  }
//
//  private def get(n: Int) = client.get(n)
//
//  implicit class FOps(f: Future[String]) {
//    def get(n: Int): Future[String] = f.flatMap(_ => client.get(n))
//
//    def put(s: String, n: Int = 1): Future[String] = (1 to n).foldLeft(f)((f, _) => f.flatMap(_ => client.put(s)))
//
//    def quit(): Future[String] = f.flatMap(_ => client.quit())
//  }
//
//  private implicit def futureToFunc(f: => Future[String]): () => Future[String] = () => f
//
//  //  override protected def beforeEach(): Unit = {
//  //    recreateClients()
//  //    super.beforeEach()
//  //  }
//
//  before(recreateClients())
//  after(closeClient())
//
//  private def recreateClients(): Unit = {
//    lowClient = Echo.newClient("localhost:" + port).toService
//    //    lowClient = ClientTcpByteBufEcho.newClient("localhost:" + port).toService
//    //    lowClient = StringClient.client.newService("localhost:" + port)
//    //      c = new TopLevelClientByteBuf(lowClient)
//    c = new TopLevelClient(lowClient)
//  }
//
//  //  override protected def afterEach(): Unit = {
//  //    closeClient()
//  //    super.afterEach()
//  //  }
//
//  private def closeClient(): Unit = Await.ready(lowClient.close(), 5.seconds)
//
//  override protected def beforeAll(): Unit = {
//    server = PutAllTogether("localhost", port).proceed(queue)
//    super.beforeAll()
//  }
//
//  override protected def afterAll(): Unit = {
//    Await.ready(server.close(), 5.seconds)
//    super.afterAll()
//  }
//}
