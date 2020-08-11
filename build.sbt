name := "ververica_task"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "net.openhft" % "chronicle-queue" % "5.19.76",
  "org.apache.commons" % "commons-lang3" % "3.11",
  "com.twitter" %% "finagle-core" % "20.7.0",
  //"com.twitter" %% "finagle-http" % "20.7.0", todo
  "com.twitter" %% "finagle-netty4" % "20.7.0",
  "commons-io" % "commons-io" % "2.7",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.2.1" % Test
)

enablePlugins(JavaServerAppPackaging)