package com.autoscout.rxscala.kafka.consumer

import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import com.autoscout.rxscala.kafka.logging.TypedLog.{KafkaPartitionsAssigned, KafkaPartitionsRevoked, OffsetCommitFailed}
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, _}
import org.apache.kafka.common.TopicPartition
import rx.lang.scala.Observable

import scala.collection.JavaConverters._
import scala.collection.mutable

object KafkaTopicObservable {

  class KafkaRecord(private val record: ConsumerRecord[String, String], private val offsetMap: mutable.Map[Int, Long]) {
    def key: String = record.key()

    def value: String = record.value()

    def partition: Int = record.partition()

    def offset: Long = record.offset()

    def commit(): Option[Long] = offsetMap.put(record.partition(), record.offset())

    def timestamp(): Long = record.timestamp()
  }

  class KafkaTopicObservableState(val topic: String,
                                  val createConsumer: () => KafkaConsumer[String, String], val commitIntervalMs:Option[Int] = None) extends ConsumerRebalanceListener with LazyLogging {

    private val offsetMap: mutable.Map[Int, Long] = new ConcurrentHashMap[Int, Long]().asScala
    private val recordsQueue: mutable.Queue[KafkaRecord] = mutable.Queue()
    private val lastCommitedAt = new AtomicLong(System.currentTimeMillis)

    private val consumer: KafkaConsumer[String, String] = {
      val kafkaConsumer = createConsumer()
      kafkaConsumer.subscribe(List(topic).asJavaCollection, this)
      kafkaConsumer
    }

    def resetConsumerOffsets(): Unit = {
      val partitionInfoList = consumer.partitionsFor(topic).asScala
      val reset: util.Map[TopicPartition, OffsetAndMetadata] =
        partitionInfoList.map(partitionInfo =>
          new TopicPartition(partitionInfo.topic(), partitionInfo.partition()) -> new OffsetAndMetadata(0)).toMap.asJava

      consumer.commitSync(reset)
    }

    def pollRecord(timeoutInMs: Long): Option[KafkaRecord] = {
      if (recordsQueue.isEmpty) {

        if(commitIntervalMs.forall(lastCommitedAt.get + _ < System.currentTimeMillis))
          flushOffsets()

        val records = consumer.poll(timeoutInMs).asScala.map(r => new KafkaRecord(r, offsetMap)).toSeq
        logger.debug(s"Received ${records.size} records from Kafka topic $topic")
        recordsQueue.enqueue(records: _*)
      }
      if (recordsQueue.nonEmpty)
        Some(recordsQueue.dequeue())
      else
        None
    }

    def finish(): Unit = {
      flushOffsets()
      logger.info(s"flushed kafka offsets (if any) for topic=$topic")
      consumer.unsubscribe()
      logger.info(s"unsubscribed kafka consumer for topic=$topic")
      consumer.close()
      logger.info(s"closed kafka consumer for topic=$topic")
    }

    def flushOffsets(): Unit = {
      if (offsetMap.nonEmpty) {
        val offsets = offsetMap.toMap.map {
          // +1 because we have to commit the next offset to consume
          case (partition, offset) => (new TopicPartition(topic, partition), new OffsetAndMetadata(offset + 1))
        }
        try {
          consumer.commitSync(offsets.asJava)
          logger.debug(s"Committed ${offsets.size} offsets to Kafka topic $topic")
          lastCommitedAt.set(System.currentTimeMillis)
        } catch {
          case e: CommitFailedException =>
            // we ignore commit failures and proceed further
            logger.error(OffsetCommitFailed(topic, e))
        } finally {
          // if the commit fails it is ok since we expect at-least-once semantics
          offsetMap.clear()
        }
      } else {
        logger.debug("No offsets to commit")
      }
    }

    override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
      logger.info(KafkaPartitionsAssigned(topic, partitions.asScala.map(_.partition()).toSeq))
    }

    override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
      logger.info(KafkaPartitionsRevoked(topic, partitions.asScala.map(_.partition()).toSeq))
      flushOffsets()
    }
  }

  def apply(topic: String, createConsumer: () => KafkaConsumer[String, String],
            commitIntervalMs:Option[Int] = None, resetOffsets: Boolean = false): Observable[KafkaRecord] =
    rx.lang.scala.JavaConversions.toScalaObservable(rx.Observable.create(new KafkaTopicSyncOnSubscribe(topic, createConsumer, commitIntervalMs, resetOffsets)))
}
