name := "ververica_task"

version := "0.1"

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.0",
  "net.openhft" % "chronicle-queue" % "5.20.3",
  "org.apache.commons" % "commons-lang3" % "3.11",
  "com.twitter" %% "finagle-netty4" % "20.7.0",
  "commons-io" % "commons-io" % "2.7",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  /* TESTS */
  "org.scalatest" %% "scalatest" % "3.2.1" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.1.0" % Test
)

enablePlugins(JavaServerAppPackaging)
