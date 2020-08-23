package task.gatling.protocol

import java.net.InetSocketAddress

import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.finagle.tracing.NullTracer
import com.twitter.util.{Duration, NullMonitor}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ChainableAction}
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import task.Tests
import task.client.finagle.TcpClient.Client
import task.client.finagle.{FinagleBaseTopLevelClient, TcpClient}

case class UpperConnectActionBuilder(requestName: String) extends ActionBuilder {
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

class UpperConnect(protocol: UpperProtocol, statsEngine: StatsEngine, val next: Action) extends ChainableAction {

  val k = new FinagleBaseTopLevelClient(
    ClientBuilder()
      .stack(TcpClient())
      .noFailureAccrual
      .monitor(_ => NullMonitor)
      .hosts(new InetSocketAddress(protocol.address, protocol.port))
      .keepAlive(true)
      .tracer(NullTracer)
      .hostConnectionLimit(20)
      .connectTimeout(Duration.fromSeconds(1)) // max time to spend establishing a TCP connection.
      .reportTo(NullStatsReceiver)
      .build()
  )

  override def execute(session: Session): Unit = {
    //    sync(session)
    async(session)
  }

  private def sync(session: Session) = {
    val start  = System.currentTimeMillis
    val resp   = k.get(1)
    val end    = System.currentTimeMillis
    val status = if (resp == Tests.InvalidReq || resp == Tests.ErrorReq) KO else OK
    statsEngine.logResponse(session, name, start, end, status, None, None)
    next ! session
  }

  private def async(session: Session) = {
    val start = System.currentTimeMillis

    k.getAsync(1).foreach { resp =>
      val end    = System.currentTimeMillis
      val status = if (resp == Tests.InvalidReq || resp == Tests.ErrorReq) KO else OK
      statsEngine.logResponse(session, name, start, end, status, None, None)
      next ! session
    }
  }

  override def name: String = "this is name" // todo
}