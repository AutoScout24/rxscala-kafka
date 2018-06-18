package com.autoscout.rxscala.kafka.consumer

import com.autoscout.rxscala.kafka.consumer.KafkaTopicObservable.{KafkaRecord, KafkaTopicObservableState}
import com.autoscout.rxscala.kafka.logging.TypedLog.{KafkaConsumerStarted, KafkaConsumerStopped}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import rx.observables.SyncOnSubscribe

import scala.util.control.NonFatal

class KafkaTopicSyncOnSubscribe(topic: String,
                                createConsumer: () => KafkaConsumer[String, String],
                                commitIntervalMs:Option[Int] = None,
                                resetOffsets: Boolean = false)
  extends SyncOnSubscribe[KafkaTopicObservableState, KafkaRecord] with LazyLogging {
  val minuteInMillis = 60000

  override def generateState(): KafkaTopicObservableState = {
    logger.info(KafkaConsumerStarted(topic))

    val state = new KafkaTopicObservableState(topic, createConsumer, commitIntervalMs)
    if (resetOffsets)
      state.resetConsumerOffsets()
    state
  }

  override def next(state: KafkaTopicObservableState,
                    observer: rx.Observer[_ >: KafkaRecord]): KafkaTopicObservableState = {
    try {
      state.pollRecord(minuteInMillis).foreach(observer.onNext)
    } catch {
      case _: WakeupException =>
        logger.warn("Kafka consumer has been woken up")
      case _: InterruptedException =>
        logger.warn("Interrupted while waiting for the messages")
        Thread.currentThread().interrupt()
      case NonFatal(e) => observer.onError(e)
    }
    state
  }

  override def onUnsubscribe(state: KafkaTopicObservableState): Unit = {
    logger.info(KafkaConsumerStopped(topic))
    state.finish()
  }
}
