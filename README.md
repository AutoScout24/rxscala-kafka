# rxscala-kafka
Library containing RxScala Observables to consume Kafka Topics

## Status
[![Build Status](https://travis-ci.org/Scout24/rxscala-kafka.svg)](https://travis-ci.org/Scout24/rxscala-kafka)

[ ![Download](https://api.bintray.com/packages/autoscout24/maven/rxscala-kafka/images/download.svg) ](https://bintray.com/autoscout24/maven/rxscala-kafka/_latestVersion)

## Setup

Add to your `build.sbt` following resolver with dependency:

```scala
resolvers += Resolver.bintrayRepo("autoscout24", "maven")

libraryDependencies += "com.autoscout24" %% "rxscala-kafka" % "(see version number above)",
```


## Configuration

For detailed list of config options please check [kafka-client documentation](https://kafka.apache.org/0100/javadoc/index.html?org/apache/kafka/clients/consumer/ConsumerConfig.html).


## Instantiating an observable


```scala
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import com.autoscout.rxscala.kafka.consumer.KafkaTopicObservable

...

def createKafkaConsumer(config: Configuration): KafkaConsumer[String, String] = {
    new KafkaConsumer[String, String](
      config.kafkaConsumerProperties
        + (CommonClientConfigs.CLIENT_ID_CONFIG -> config.kafkaClientName)
        + (ConsumerConfig.GROUP_ID_CONFIG -> config.consumerGroupId)
    )
  }

val kafkaObservable = KafkaTopicObservable(config.listingsTopicName, () => createKafkaConsumer(config))

val notNullRecords = kafkaObservable.filter(_.value != null)


```
