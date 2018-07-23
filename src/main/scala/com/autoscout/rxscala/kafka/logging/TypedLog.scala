package com.autoscout.rxscala.kafka.logging

import com.typesafe.scalalogging.Logger
import TypedLog.StringHelper

import scala.language.implicitConversions
import net.logstash.logback.marker.{LogstashMarker, Markers}

import scala.collection.JavaConverters._

sealed trait TypedLog extends Product {

  /**
    * Require all logs to have a standard message field
    */
  val message: String

  /**
    * store the `type` as string for the current log
    */
  final val typeName: String = StringHelper.camelToHyphens(this.getClass.getSimpleName)

  def toMap: Map[String, Any] =
    this.getClass.getDeclaredFields
      .map(_.getName) // all field names
      .zip(this.productIterator.to)
      .toMap // zipped with all values

}

object TypedLog {
  case class KafkaConsumerStarted(topic: String) extends TypedLog {
    override val message: String = s"Kafka consumer started for the topic $topic"
  }

  case class KafkaConsumerStopped(topic: String) extends TypedLog {
    override val message: String = s"Kafka consumer stopped for the topic $topic"
  }

  case class KafkaPartitionsAssigned(topic: String, partitions: Seq[Int]) extends TypedLog {
    override val message: String = s"Kafka partitions assigned for the topic $topic: $partitions"
  }

  case class KafkaPartitionsRevoked(topic: String, partitions: Seq[Int]) extends TypedLog {
    override val message: String = s"Kafka partitions revoked for the topic $topic: $partitions"
  }

  case class OffsetCommitFailed(topic: String, e: Throwable) extends TypedLog {
    override val message: String = s"Failed to commit offsets for the topic $topic"
  }

  /**
    * Extension method: Extend regular logback logger
    */
  implicit class LoggerEnhancer(logger: Logger) {

    private def typedLogToMarkerConverterExcludingMessage(typedLog: TypedLog): LogstashMarker =
      Markers.appendEntries(
        (
          typedLog.toMap
            .filterKeys(_ != "message") + ("type" -> typedLog.typeName) // add `type` field to list of values
          ).asJava
      )

    def trace(typedLog: TypedLog): Unit =
      logger.trace(typedLogToMarkerConverterExcludingMessage(typedLog), typedLog.message)
    def debug(typedLog: TypedLog): Unit =
      logger.debug(typedLogToMarkerConverterExcludingMessage(typedLog), typedLog.message)
    def info(typedLog: TypedLog): Unit =
      logger.info(typedLogToMarkerConverterExcludingMessage(typedLog), typedLog.message)
    def warn(typedLog: TypedLog): Unit =
      logger.warn(typedLogToMarkerConverterExcludingMessage(typedLog), typedLog.message)
    def error(typedLog: TypedLog): Unit =
      logger.error(typedLogToMarkerConverterExcludingMessage(typedLog), typedLog.message)

    def error(typedLog: TypedLog, cause: Throwable): Unit =
      logger.error(typedLogToMarkerConverterExcludingMessage(typedLog), typedLog.message, cause)
  }

  object StringHelper {
    def camelToHyphens(string: String) =
      "[A-Z\\d]".r
        .replaceAllIn(string, { m =>
          "-" + m.group(0).toLowerCase()
        })
        .stripPrefix("-")
  }

}
