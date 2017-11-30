package consumer

import java.util.Properties

import consumer.KafkaTopicObservable.KafkaTopicObservableState
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}


class KafkaTopicObservableStateTest extends WordSpec with MustMatchers with MockitoSugar {
  "KafkaTopicObservableState" should {
    "create a consumer when created" in {
      val mockConsumer = mock[() => KafkaConsumer[String, String]]
      // stubbed unit tests to quickly add to later
    }
  }
}
