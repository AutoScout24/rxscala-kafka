package consumer

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.{ConsumerRebalanceListener, _}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal
import com.autoscout24.gecloud.gaspedaal.exporter.logging.TypedLog._
import consumer.KafkaTopicObservable.{KafkaRecord, KafkaTopicObservableState}
import rx.observables.SyncOnSubscribe
import rx.lang.scala.Observable

class KafkaTopicSyncOnSubscribe(topic: String,
                              createConsumer: () => KafkaConsumer[String, String], resetOffsets: Boolean = false)
  extends SyncOnSubscribe[KafkaTopicObservableState, KafkaRecord] with LazyLogging {

  override def generateState(): KafkaTopicObservableState = {
    logger.info(KafkaConsumerStarted(topic))

    val state = new KafkaTopicObservableState(topic, createConsumer)
    if (resetOffsets)
      state.resetConsumerOffsets()
    state
  }

  override def next(state: KafkaTopicObservableState,
                    observer: rx.Observer[_ >: KafkaRecord]): KafkaTopicObservableState = {
    try {
      state.pollRecord(60000).foreach(observer.onNext)
    } catch {
      case _: WakeupException => logger.warn("Kafka consumer has been woken up")
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

object KafkaTopicObservable {

  class KafkaRecord(private val record: ConsumerRecord[String, String], private val offsetMap: mutable.Map[Int, Long]) {
    def key: String = record.key()

    def value: String = record.value()

    def partition: Int = record.partition()

    def offset: Long = record.offset()

    def commit(): Unit = offsetMap.put(record.partition(), record.offset())

    def timestamp(): Long = record.timestamp()
  }

  class KafkaTopicObservableState(val topic: String,
                                  val createConsumer: () => KafkaConsumer[String, String]) extends ConsumerRebalanceListener with LazyLogging {

    private val offsetMap: mutable.Map[Int, Long] = new ConcurrentHashMap[Int, Long]()
    private val recordsQueue: mutable.Queue[KafkaRecord] = mutable.Queue()

    private val consumer: KafkaConsumer[String, String] = {
      val kafkaConsumer = createConsumer()
      kafkaConsumer.subscribe(List(topic).asJavaCollection, this)
      kafkaConsumer
    }

    def resetConsumerOffsets(): Unit = {
      val partitions = consumer.partitionsFor(topic)
      val reset: util.Map[TopicPartition, OffsetAndMetadata] =
        partitions.map(x => new TopicPartition(x.topic(), x.partition()) -> new OffsetAndMetadata(0)).toMap.asJava

      consumer.commitSync(reset)
    }

    def pollRecord(timeoutInMs: Long): Option[KafkaRecord] = {
      if (recordsQueue.isEmpty) {
        flushOffsets()
        val records = consumer.poll(timeoutInMs).map(r => new KafkaRecord(r, offsetMap)).toSeq
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
          case (partition, offset) => (new TopicPartition(topic, partition), new OffsetAndMetadata(offset + 1))
          // +1 because we have to commit the next offset to consume
        }
        try {
          consumer.commitSync(offsets)
          logger.debug(s"Committed ${offsets.size} offsets to Kafka topic $topic")
        } catch {
          case e: CommitFailedException =>
            // we ignore commit failures and proceed further
            logger.error(OffsetCommitFailed(topic, e))
        } finally {
          offsetMap.clear() // if the commit fails it is ok since we expect at-least-once semantic
        }
      } else {
        logger.debug("No offsets to commit")
      }
    }

    override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit = {
      logger.info(KafkaPartitionsAssigned(topic))
    }

    override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit = {
      logger.info(KafkaPartitionsRevoked(topic))
      flushOffsets()
    }
  }

  def apply(topic: String, createConsumer: () => KafkaConsumer[String, String],
            resetOffsets: Boolean = false): Observable[KafkaRecord] =
    rx.lang.scala.JavaConversions.toScalaObservable(rx.Observable.create(new KafkaTopicSyncOnSubscribe(topic, createConsumer)))
}
