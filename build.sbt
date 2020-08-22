name := "ververica_task"

version := "0.1"

scalaVersion := "2.12.11"

val `akka-version` = "2.6.8"
val `gatling-version` = "3.3.1"
val `finagle-version` = "20.7.0"

val AkkaVersion = "2.6.8"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion

libraryDependencies += "com.google.guava" % "guava" % "29.0-jre"

libraryDependencies ++= Seq(
//  "net.openhft" % "chronicle-queue" % "5.19.76",
  "net.openhft" % "chronicle-queue" % "5.20.3",
  "org.apache.commons" % "commons-lang3" % "3.11",
  "com.twitter" %% "finagle-core" % `finagle-version`,

//  "com.twitter" %% "finagle-http" % "20.7.0", // todo

  "com.twitter" %% "finagle-netty4" % `finagle-version`,
  "commons-io" % "commons-io" % "2.7",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.2.1" % Test,


  "io.gatling" % "gatling-app" % `gatling-version` % Test,
  "io.gatling.highcharts" % "gatling-charts-highcharts" % `gatling-version` % Test
    exclude("io.gatling", "gatling-recorder"),
  "com.typesafe.akka" %% "akka-slf4j" % `akka-version` % Test,
  "com.typesafe.akka" %% "akka-stream" % `akka-version` % Test,
  "io.gatling" % "gatling-test-framework" % `gatling-version` % Test
)

libraryDependencies += "io.projectreactor.netty" % "reactor-netty" % "0.9.11.RELEASE" // todo
libraryDependencies += "io.projectreactor" % "reactor-test" % "3.3.9.RELEASE" % Test
libraryDependencies += "org.scalatestplus" %% "mockito-3-4" % "3.2.1.0"



enablePlugins(JavaServerAppPackaging)
enablePlugins(JmhPlugin)
enablePlugins(GatlingPlugin)