ThisBuild / scalaVersion     := "2.13.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "boost-kafka-lag"

enablePlugins(GraalVMNativeImagePlugin)

graalVMNativeImageOptions := Seq(
  "--no-fallback",
  "--allow-incomplete-classpath",
  "--verbose",
  "--report-unsupported-elements-at-runtime",
  "-H:+ReportExceptionStackTraces"
)
val http4sVersion = "0.21.0-M6"
lazy val root = (project in file("."))
  .settings(
    name := "boost-kafka-lag",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      "org.slf4j" % "slf4j-api" % "1.7.30",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.ovoenergy" %% "fs2-kafka" % "0.20.2",
      "org.http4s" %% "http4s-async-http-client" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
    )

  )
