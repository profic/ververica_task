package task.gatling.testrunner

import scala.concurrent.duration._
import scala.language.postfixOps

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import task.gatling.protocol.{UpperConnectActionBuilder, UpperProtocol}

class ExampleSimulation extends Simulation {
  val scn: ScenarioBuilder = scenario("test")
    .exec(UpperConnectActionBuilder("user"))

//  setUp(scn.inject(constantUsersPerSec(1000).during(1.minutes))).throttle(
//    reachRps(100000).in(10.minute),
//    holdFor(1.minute),
//    jumpToRps(1000),
//    holdFor(2.minute)
//  ).protocols(UpperProtocol("localhost", 10042))

  setUp(scn.inject(constantUsersPerSec(100).during(5.minutes)))
    .throttle(
    reachRps(100).in(2.minute),
    holdFor(1.minute),
    jumpToRps(10),
    holdFor(2.minute)
  ).protocols(UpperProtocol("localhost", 10042))

//  setUp(scn.inject(atOnceUsers(20)))
//    .protocols(UpperProtocol("localhost", 10042))
}
