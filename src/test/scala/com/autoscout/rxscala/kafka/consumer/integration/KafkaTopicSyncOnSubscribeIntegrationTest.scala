package com.autoscout.rxscala.kafka.consumer.integration

import com.autoscout.rxscala.kafka.consumer.KafkaTopicObservable
import com.autoscout.rxscala.kafka.consumer.KafkaTopicObservable.KafkaRecord
import com.autoscout.rxscala.kafka.consumer.integration.TestHelper._
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}
import rx.lang.scala.observers.TestSubscriber

import scala.util.control.Exception.handling

class KafkaTopicSyncOnSubscribeIntegrationTest extends FlatSpec with BeforeAndAfterAll with MustMatchers with MockitoSugar {

  override protected def beforeAll(): Unit = {
    EmbeddedKafka.start()
    sendSampleRawListingsMessagesToKafka("test")
  }

  "consumer" should "consume listings from kafka topic up to a limit and count errors and success" in {
    val createConsumer = () => new KafkaConsumer[String, String](Configuration.kafkaConsumerProperties)
    val testSubscriber: TestSubscriber[KafkaRecord] = TestSubscriber[KafkaRecord]
    val kafkaObservable = KafkaTopicObservable("test", createConsumer)
    kafkaObservable.take(63).toBlocking.subscribe(testSubscriber)
    testSubscriber.assertNoErrors()
    testSubscriber.getOnNextEvents.size mustBe 63
  }

  "consumer also" should "unsubscribe appropriately" in {
    val createConsumer = () => new KafkaConsumer[String, String](Configuration.kafkaConsumerProperties)
    val testSubscriber: TestSubscriber[KafkaRecord] = TestSubscriber[KafkaRecord]
    val kafkaObservable = KafkaTopicObservable("test", createConsumer)
    kafkaObservable.take(1).toBlocking.subscribe(testSubscriber)
    testSubscriber.assertNoErrors()
    testSubscriber.isUnsubscribed mustBe false
    testSubscriber.unsubscribe()
    testSubscriber.isUnsubscribed mustBe true
  }

  override protected def afterAll(): Unit = {
    ignoreError {
      EmbeddedKafka.stop()
    }
  }

  private val ignoreError = handling(classOf[Exception]) by (_.printStackTrace())

}
