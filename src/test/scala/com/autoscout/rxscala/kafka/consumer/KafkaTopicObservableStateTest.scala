package com.autoscout.rxscala.kafka.consumer

import com.autoscout.rxscala.kafka.consumer.KafkaTopicObservable.KafkaTopicObservableState
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{RETURNS_DEEP_STUBS, times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}

import scala.collection.JavaConverters._


class KafkaTopicObservableStateTest extends WordSpec with MustMatchers with MockitoSugar {

  class TestFixture {
    val mockConsumerFunc = mock[() => KafkaConsumer[String, String]]
    val mockConsumer = mock[KafkaConsumer[String, String]]
    when(mockConsumerFunc.apply()).thenReturn(mockConsumer)

    val consumerRecord = mock[ConsumerRecords[String, String]](RETURNS_DEEP_STUBS)
    when(mockConsumer.poll(any())).thenReturn(mock[ConsumerRecords[String, String]](RETURNS_DEEP_STUBS))

    val topic = "testTopic"
    val kafkaTopicObservable = new KafkaTopicObservableState(topic, mockConsumerFunc)
  }

  "KafkaTopicObservableState" should {
    "instantiate a consumer and subscribe to it when created" in new TestFixture {
      verify(mockConsumerFunc, times(1)).apply()
      verify(mockConsumer, times(1)).subscribe(List(topic).asJavaCollection, kafkaTopicObservable)
    }

    "create a consumer when created" in new TestFixture {
      kafkaTopicObservable.resetConsumerOffsets()
      verify(mockConsumer, times(1)).commitSync(any())
    }

    "return None when records queue is empty" in new TestFixture {
      kafkaTopicObservable.pollRecord(1) mustBe None
    }

    "returns Some when records queue is not empty" in new TestFixture {
      val mockRecords = mock[ConsumerRecords[String, String]](RETURNS_DEEP_STUBS)
      val testConsumerRecord = new ConsumerRecord[String, String](topic, 0, 0, "Test", "Test")
      val consumerRecords = new ConsumerRecords[String, String](Map((new TopicPartition(topic, 0), List(testConsumerRecord).asJava)).asJava)

      when(mockConsumer.poll(1)).thenReturn(consumerRecords)
      when(mockRecords.records(topic)).thenReturn(List(testConsumerRecord).asJava)
      kafkaTopicObservable.pollRecord(1) mustBe a[Some[_]]
    }
  }
}
