package task.gatling.testrunner

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

object GatlingRunner {
  def main(args: Array[String]) {

    val simClass = classOf[TaskSimulation].getName

    val props = new GatlingPropertiesBuilder()
      .simulationClass(simClass)
      .resultsDirectory("D:\\tmp")

    Gatling.fromMap(props.build)
  }
}
