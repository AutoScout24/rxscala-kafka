package com.autoscout.rxscala.kafka.consumer.integration

import java.util.concurrent.TimeUnit

import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import play.api.libs.json.{JsString, Json}
import rx.lang.scala.Observable

import scala.io.Source
import scala.util.Try

object TestHelper {
  val sendSampleRawListingsMessagesToKafka = new SendSampleMessagesToKafka("/sample-messages.json", "test")

  class SendSampleMessagesToKafka(sampleFileResourcePath: String, inputTopicName: String) extends ((String) => Unit) with LazyLogging {
    override def apply(inputTopicName: String): Unit = {

      println(s"Sending sample messages to ${inputTopicName} topic.")

      val lines = Try(Source.fromInputStream(getClass.getResourceAsStream(sampleFileResourcePath)).getLines()).get
      val producer = new KafkaProducer[String, String](Configuration.kafkaProducerProperties)
      try {
        Observable.from(lines.toIterable)
          .map(l => (extractClassifiedGuid(l), l))
          .doOnNext(l => producer.send(new ProducerRecord[String, String](inputTopicName, l._1, l._2)).get(10, TimeUnit.SECONDS))
          .tumblingBuffer(10)
          .doOnNext(lines => logger.info(s"published ${lines.size} listings to Kafka Topic $inputTopicName"))
          .subscribe()
      } finally {
        producer.close()
      }
    }

    private def extractClassifiedGuid(listing: String): String = {
      def tryIdField(field: String) = Try((Json.parse(listing) \ field).get.asInstanceOf[JsString].value)
      tryIdField("id")
        .get
    }
  }
}
