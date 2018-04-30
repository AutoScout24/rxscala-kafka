package com.autoscout.rxscala_kafka.consumer

import com.autoscout.rxscala_kafka.consumer.KafkaTopicObservable.{KafkaRecord, KafkaTopicObservableState}
import org.apache.kafka.clients.consumer.{ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.errors.WakeupException
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import rx.Observer

import scala.collection.JavaConverters._

class KafkaTopicSyncOnSubscribeTest extends WordSpec with MockitoSugar with MustMatchers {
  trait TestFixture {
    val topicName = "test"
    val consumerMock = mock[KafkaConsumer[String, String]]
    val observerMock = mock[Observer[KafkaRecord]]
    val stateMock = new KafkaTopicObservableState(topicName, () => consumerMock)
    val topicSource = new KafkaTopicSyncOnSubscribe(topicName, () => consumerMock)
  }

  trait ConsumerReturningRecords extends TestFixture {
    val recordsMock = mock[KafkaRecord]
    val consumerRecordsMock = mock[ConsumerRecords[String, String]]
    when(consumerMock.poll(any())).thenReturn(consumerRecordsMock)
  }

  "generateState" when {
    "stopped" should {
      "do no subscription" in new TestFixture {
        topicSource.generateState()
        verify(consumerMock, never()).subscribe(any(classOf[java.util.Collection[String]]))
      }
    }

    "not stopped" should {
      "do a subscription to the topic" in new TestFixture {
        val currentState = topicSource.generateState()
        verify(consumerMock).subscribe(List(topicName).asJavaCollection, currentState)
      }
    }
  }

  "next" when {
    "consumer is woken up" should {
      "do nothing" in new TestFixture {
        when(consumerMock.poll(any())).thenThrow(new WakeupException)
        topicSource.next(stateMock, observerMock)
        verifyZeroInteractions(observerMock)
      }
    }

    "the consumer gives back records" should {
      "give them to the observer" in new ConsumerReturningRecords {
        val recordStateMock: KafkaTopicObservableState = mock[KafkaTopicObservableState]
        when(recordStateMock.pollRecord(any())).thenReturn(Some(recordsMock))
        topicSource.next(recordStateMock, observerMock) mustBe recordStateMock
        verify(observerMock).onNext(eqTo(recordsMock))
      }
    }
  }

  "onUnsubscribe" should {
    "commit offsets" in new TestFixture {
      val unSubscribeStateMock = mock[KafkaTopicObservableState]
      topicSource.onUnsubscribe(unSubscribeStateMock)
      verify(unSubscribeStateMock).finish()
    }

    "close the consumer if stopped" in new TestFixture {
      topicSource.onUnsubscribe(stateMock)
      verify(consumerMock).unsubscribe()
      verify(consumerMock).close()
    }
  }
}
