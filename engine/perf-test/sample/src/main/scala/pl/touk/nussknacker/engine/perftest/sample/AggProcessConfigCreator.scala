package pl.touk.nussknacker.engine.perftest.sample

import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema
import pl.touk.nussknacker.engine.api.exception.ExceptionHandlerFactory
import pl.touk.nussknacker.engine.api.process.{ProcessConfigCreator, WithCategories}
import pl.touk.nussknacker.engine.api.test.TestParsingUtils
import pl.touk.nussknacker.engine.flink.util.exception.VerboselyLoggingExceptionHandler
import pl.touk.nussknacker.engine.kafka.{KafkaConfig, KafkaSinkFactory, KafkaSourceFactory}
import pl.touk.nussknacker.engine.perftest.sample.model.KeyValue
import pl.touk.nussknacker.engine.util.LoggingListener
import pl.touk.nussknacker.engine.flink.util.source.CsvSchema

class AggProcessConfigCreator extends ProcessConfigCreator {

  import org.apache.flink.streaming.api.scala._

  override def listeners(config: Config) = List(LoggingListener)

  override def sourceFactories(config: Config) = {
    val kafkaConfig = config.as[KafkaConfig]("kafka")
    Map(
      "kafka-keyvalue" -> WithCategories(new KafkaSourceFactory[KeyValue](
        kafkaConfig,
        new CsvSchema(KeyValue.apply),
        Some(new BoundedOutOfOrdernessTimestampExtractor[KeyValue](Time.minutes(10)) { // this number depends on batch fetched by consumer
          override def extractTimestamp(element: KeyValue) = element.date.getTime
        }),
        TestParsingUtils.newLineSplit
      ))
    )
  }

  override def sinkFactories(config: Config) = {
    val kafkaConfig = config.as[KafkaConfig]("kafka")
    val intSerializationSchema = new KeyedSerializationSchema[Any] {

      override def serializeValue(element: Any) = {
        element.asInstanceOf[Int].toString.getBytes(StandardCharsets.UTF_8)
      }

      override def serializeKey(element: Any) =
        null

      override def getTargetTopic(element: Any) =
        null

    }

    Map(
      "kafka-int" -> WithCategories(new KafkaSinkFactory(kafkaConfig, intSerializationSchema))
    )
  }

  override def services(config: Config) = Map.empty

  override def customStreamTransformers(config: Config) = Map()

  override def exceptionHandlerFactory(config: Config) =
    ExceptionHandlerFactory.noParams(VerboselyLoggingExceptionHandler(_))

  override def globalProcessVariables(config: Config) = Map.empty


  override def signals(config: Config) = Map.empty

  override def buildInfo(): Map[String, String] = Map.empty
}