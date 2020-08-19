package task.gatling.protocol

import java.util.UUID

import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.action.{Action, ChainableAction}
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import task.Tests

case class UpperConnectActionBuilder(requestName: String) extends ActionBuilder {
  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry) = {
    protocolComponentsRegistry.components(task.gatling.protocol.UpperProtocol.UpperProtocolKey)
  }

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val statsEngine = coreComponents.statsEngine
    val upperComponents = components(protocolComponentsRegistry)
    new UpperConnect(upperComponents.upperProtocol, statsEngine, next)
  }
}

class UpperConnect(protocol: task.gatling.protocol.UpperProtocol, val statsEngine: StatsEngine, val next: Action) extends ChainableAction {

  override def execute(session: Session): Unit = {
    val k = new UpperServiceClient(protocol.address, protocol.port)
    val start = System.currentTimeMillis
    val resp = k.run()
    val end = System.currentTimeMillis
    val status =
      if (resp == Tests.invalidRequestDef || resp == Tests.error) KO
      else OK
    statsEngine.logResponse(session, name, start, end, status, None, None)
    next ! session
  }

//  override def name: String = UUID.randomUUID().toString
  override def name: String = "this is name"
}