package task.gatling.testrunner

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import task.client.netty.{NettyBaseTopLevelClient, ScalaClientNetty}
import task.gatling.protocol.{UpperConnectActionBuilder, UpperProtocol}

import scala.concurrent.duration._
import scala.language.postfixOps

class ExampleSimulation extends Simulation {

  private val protocol = UpperProtocol("localhost", 10042)

  //  val scn: ScenarioBuilder = scenario("get one line")
  //    .during(1.minute)(ChainBuilder(List(UpperConnectActionBuilder(""))))

  //  setUp(scn.inject(constantUsersPerSec(1000).during(1.minutes))).throttle(
  //    reachRps(100000).in(10.minute),
  //    holdFor(1.minute),
  //    jumpToRps(1000),
  //    holdFor(2.minute)
  //  ).protocols(UpperProtocol("localhost", 10042))

  //  private val reachPrs = 100000
  //  private val fromUsers = 10
  //  private val toUsers = 20
  //  private val jumpToRpsCnt = 10000

  private val scn = scenario("get one line")
    .exec(UpperConnectActionBuilder(""))

  //  setUp(scn.inject(rampConcurrentUsers(fromUsers).to(toUsers).during(5.minutes)))

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

  //  setUp(scn.inject(atOnceUsers(20)))
  //    .protocols(UpperProtocol("localhost", 10042))
}
