package integration

import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}

import scala.util.control.Exception.handling

class KafkaTopicSyncOnSubscribeIntegrationTest extends FlatSpec with BeforeAndAfterAll with MustMatchers with MockitoSugar {

  override protected def beforeAll(): Unit = {
    EmbeddedKafka.start()(EmbeddedKafkaConfig(customBrokerProperties = Map("num.partitions" -> "16")))
    //sendSampleRawListingsMessagesToKafka(config)
  }

  override protected def afterAll(): Unit = {
    ignoreError {
      EmbeddedKafka.stop()
    }
  }

  private val ignoreError = handling(classOf[Exception]) by (_.printStackTrace())

}
