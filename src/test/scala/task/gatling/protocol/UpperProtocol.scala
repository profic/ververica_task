package task.gatling.protocol

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolComponents, ProtocolKey}
import io.gatling.core.session.Session

case class UpperProtocol(address: String, port: Int) extends Protocol

object UpperProtocol {
  val UpperProtocolKey: ProtocolKey[UpperProtocol, UpperComponents] = new ProtocolKey[UpperProtocol, UpperComponents] {

    override def protocolClass: Class[Protocol] = classOf[UpperProtocol].asInstanceOf[Class[Protocol]]

    override def defaultProtocolValue(configuration: GatlingConfiguration): UpperProtocol = {
      throw new IllegalStateException("Can't provide a default value for UpperProtocol")
    }

    //		override def newComponents(system: ActorSystem, coreComponents: CoreComponents): UpperProtocol => UpperComponents = {
    //			upperProtocol => UpperComponents(upperProtocol)
    //		}
    override def newComponents(coreComponents: CoreComponents): UpperProtocol => UpperComponents = {
      upperProtocol => UpperComponents(upperProtocol)
    }
  }
}

case class UpperComponents(upperProtocol: UpperProtocol) extends ProtocolComponents {
  override def onStart: Session => Session = ProtocolComponents.NoopOnStart
  override def onExit: Session => Unit = ProtocolComponents.NoopOnExit
}

case class UpperProtocolBuilder(address: String, port: Int) {
  def build(): UpperProtocol = UpperProtocol(address, port)
}
