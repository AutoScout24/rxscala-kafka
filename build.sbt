name := "rxscala-kafka"
organization := "com.autoscout24"
organizationName := "AutoScout24"
organizationHomepage := Some(url("https://www.autoscout24.de/"))
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayOrganization := Some("autoscout24")

version := "1.0"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.12.4", "2.11.7")

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

javaOptions in (ThisBuild, Test) ++= Seq(
  "-Dconfig.resource=test.conf",
  "-Dlogback.configurationFile=as24local-logger.xml"
)
fork in Test := true

val kafkaVersion = "0.10.1.1"
val sl4jVersion = "1.7.21"

val testDependencies = Seq(
  "org.mockito" % "mockito-all" % "1.10.19" % Test,
  "net.manub" %% "scalatest-embedded-kafka" % "0.11.0" % Test excludeAll(
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "log4j", name = "log4j"),
    ExclusionRule(organization = "org.apache.kafka")
  ),
  "org.apache.kafka" %% "kafka" % kafkaVersion % Test excludeAll(
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "log4j", name = "log4j")
  )
)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % sl4jVersion,
  "org.slf4j" % "log4j-over-slf4j" % sl4jVersion, // Kafka uses log4j
  "org.scalatest"  %% "scalatest" % "3.0.1" % Test,
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "com.typesafe" % "config" % "1.3.0",
  "io.reactivex" % "rxscala_2.11" % "0.26.2",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % "2.8.5",
  "org.eclipse.jetty" % "example-jetty-embedded" % "9.3.6.v20151106" exclude("org.eclipse.jetty.tests", "test-mock-resources"),
  "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.2",
  "org.apache.kafka" % "kafka-clients" % kafkaVersion
) ++ testDependencies