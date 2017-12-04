package integration

import java.net.InetAddress
import java.util.Properties

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig

object Configuration {

  private lazy val config = ConfigFactory.load()

  lazy val topicName: String = config.getString("test")

  lazy val kafkaPort: Int = config.getInt("kafka.port")
  lazy val brokers: String = InetAddress.getAllByName(config.getString("kafka.host"))
    .map(a => s"${a.getHostAddress}:$kafkaPort").mkString(",")
  lazy val kafkaClientName: String = config.getString("kafka.client.name")
  lazy val maxPollMessages: String = config.getString("kafka.consumer.max-poll-messages")

  private[integration] def kafkaConsumerProperties: Properties = {
    val kafkaConsumerProperties = kafkaBaseConfig
    kafkaConsumerProperties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "20000")
    kafkaConsumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "20000")
    kafkaConsumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
    kafkaConsumerProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollMessages)
    kafkaConsumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    kafkaConsumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    kafkaConsumerProperties
  }

  private[integration] def kafkaProducerProperties: Properties = {
    val config = kafkaBaseConfig
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    config.put(ProducerConfig.LINGER_MS_CONFIG, "50")
    config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "10000")
    config.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, "5000")
    config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, "671088640") // 160 mb
    config.put(ProducerConfig.BATCH_SIZE_CONFIG, "524288")
    config.put(ProducerConfig.ACKS_CONFIG, "all")
    config
  }

  private[integration] def kafkaBaseConfig: Properties = {
    val kafkaBaseConfig = new Properties()
    kafkaBaseConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokers)
    kafkaBaseConfig.put(CommonClientConfigs.CLIENT_ID_CONFIG, kafkaClientName)
    kafkaBaseConfig.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, "25000")
    kafkaBaseConfig
  }
}
