package task.gatling.testrunner

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object GatlingRunner {
  def main(args: Array[String]) {

//    task.netty.NettyServerScala.serverForGatling()

    val simClass = classOf[ExampleSimulation].getName

    val props = new GatlingPropertiesBuilder
    props.simulationClass(simClass)
    Gatling.fromMap(props.build)
  }
}
