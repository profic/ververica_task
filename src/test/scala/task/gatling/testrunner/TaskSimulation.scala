package task.gatling.testrunner

import io.gatling.core.Predef._
import task.gatling.protocol.{UpperConnectActionBuilder, UpperProtocol}

import scala.concurrent.duration._
import scala.language.postfixOps

class TaskSimulation extends Simulation {

  private val protocol = UpperProtocol("localhost", 10042)

  private val scn = scenario("get one line")
    .exec(UpperConnectActionBuilder(""))

  private val (reachPrs, fromUsers, toUsers, jumpToRpsCnt) = (
    //    10, 10, 20, 5
    100000, 10, 20, 10000
  )

  setUp(scn.inject(rampConcurrentUsers(1).to(20).during(1.minute)))
    //    .throttle(
    //      reachRps(reachPrs).in(2.minute),
    //      holdFor(1.minute),
    //      jumpToRps(jumpToRpsCnt),
    //      holdFor(2.minute)
    //    )
    .protocols(protocol)
}
