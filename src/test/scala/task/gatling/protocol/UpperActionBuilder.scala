package task.gatling.protocol

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.util.{Duration, FuturePool, NullMonitor}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ChainableAction}
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.netty.util.concurrent.Future
import task.Tests
import task.client.finagle.Echo.Client
import task.client.finagle.{Echo, FinagleBaseTopLevelClient}
import task.client.netty.{NettyBaseTopLevelClient, ScalaClientNetty}

import scala.concurrent.Await
import scala.concurrent.duration._

object UpperConnectActionBuilder {
  val cnt = new AtomicInteger()
}

case class UpperConnectActionBuilder(requestName: String) extends ActionBuilder {
  println(s"UpperConnectActionBuilder.cnt.incrementAndGet() = ${UpperConnectActionBuilder.cnt.incrementAndGet()}")
  private def components(registry: ProtocolComponentsRegistry) = {
    registry.components(UpperProtocol.UpperProtocolKey)
  }

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val statsEngine     = coreComponents.statsEngine
    val upperComponents = components(protocolComponentsRegistry)
    new UpperConnect(upperComponents.upperProtocol, statsEngine, next)
  }
}

object UpperConnect {
  val cnt = new AtomicInteger()
}

class UpperConnect(protocol: UpperProtocol, statsEngine: StatsEngine, val next: Action) extends ChainableAction {

  //  private val k = new FinagleBaseTopLevelClient(Echo.newClient(s"${protocol.address}${protocol.port}").toService)

  val k = new FinagleBaseTopLevelClient(
    ClientBuilder()
      .stack(Client())
      .noFailureAccrual
      .monitor(_ => NullMonitor)
      .hosts(new InetSocketAddress(protocol.address, protocol.port))
      .keepAlive(true)
      .tracer(NullTracer)
      .hostConnectionLimit(20)
      .connectTimeout(Duration.fromSeconds(1)) // max time to spend establishing a TCP connection.
      .reportTo(NullStatsReceiver) // export host-level load data to the loaded-StatsReceiver
      .build()
  )

  println(s"UpperConnect.cnt.incrementAndGet() = ${UpperConnect.cnt.incrementAndGet()}")

  override def execute(session: Session): Unit = {
    //    sync(session)
    async(session)
  }

  //  private val client = {
  //    val netty = new ScalaClientNetty(protocol.address, protocol.port)
  //    Await.ready(netty.startClient(), 5.seconds)
  //    new NettyBaseTopLevelClient(netty)
  //  }
  //
  //  override def execute(session: Session): Unit = {
  //
  //    val start = System.currentTimeMillis
  //
  //    client.get(1).addListener((_: Future[_ >: Void]) => {
  //      //      val status = if (resp == Tests.invalidRequestDef || resp == Tests.error) KO else OK
  //      val status = OK
  //      val end    = System.currentTimeMillis
  //
  //      statsEngine.logResponse(session, name, start, end, status, None, None)
  //      next ! session
  //    }
  //    )
  //  }

  private def sync(session: Session) = {
    val start  = System.currentTimeMillis
    val resp   = k.get(1)
    val end    = System.currentTimeMillis
    val status = if (resp == Tests.invalidRequestDef || resp == Tests.error) KO else OK
    statsEngine.logResponse(session, name, start, end, status, None, None)
    next ! session
  }

  private def async(session: Session) = {
    val start = System.currentTimeMillis

    k.getAsync(1).foreach { resp =>
      val end    = System.currentTimeMillis
      val status = if (resp == Tests.invalidRequestDef || resp == Tests.error) KO else OK
      statsEngine.logResponse(session, name, start, end, status, None, None)
      next ! session
    }
  }

  override def name: String = "this is name" // todo
}