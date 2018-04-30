name := "rxscala-kafka"
organization := "com.autoscout24"
organizationName := "AutoScout24"
organizationHomepage := Some(url("https://www.autoscout24.de/"))
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayOrganization := Some("autoscout24")

version := "1.0.1"

scalaVersion := "2.12.6"
crossScalaVersions := Seq("2.12.6", "2.11.12")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

fork in Test := true

val versions = new {
  val kafka = "1.1.0"
  val slf4j = "1.7.25"
  val scalaLogging = "3.9.0"
  val logstashLogbackEncoder = "4.11"
  val typesafeConfig = "1.3.3"
  val rxJava = "1.3.8"
  val rxScala = "0.26.5"
  val scalatest = "3.0.5"
  val mockito = "2.18.3"
  val logback = "1.2.3"
  val playJson = "1.1.7"
}

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % versions.slf4j,
  "org.slf4j" % "log4j-over-slf4j" % versions.slf4j, // Kafka uses log4j
  "com.typesafe.scala-logging" %% "scala-logging" % versions.scalaLogging,
  "net.logstash.logback" % "logstash-logback-encoder" % versions.logstashLogbackEncoder,
  "com.typesafe" % "config" % versions.typesafeConfig,
  "io.reactivex" % "rxjava" % versions.rxJava,
  "io.reactivex" %% "rxscala" % versions.rxScala,
  "org.apache.kafka" % "kafka-clients" % versions.kafka,
  "org.scalatest" %% "scalatest" % versions.scalatest % Test,
  "org.mockito" % "mockito-core" % versions.mockito % Test,
  "ch.qos.logback" % "logback-classic" % versions.logback % Test,
  "com.typesafe.play" %% "play-ws-standalone-json" % versions.playJson % Test,
  "net.manub" %% "scalatest-embedded-kafka" % versions.kafka % Test excludeAll(
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "log4j", name = "log4j"),
    ExclusionRule(organization = "org.apache.kafka")
  ),
  "org.apache.kafka" %% "kafka" % versions.kafka % Test excludeAll(
    ExclusionRule(organization = "org.slf4j", name = "slf4j-log4j12"),
    ExclusionRule(organization = "log4j", name = "log4j")
  )
)
