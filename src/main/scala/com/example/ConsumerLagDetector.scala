package com.example

import java.time.Instant
import java.util.concurrent.TimeUnit

import fs2.Stream
import cats.effect._
import cats.implicits._
import com.example.metric.influxdb.InfluxdbMetricWriter
import com.example.metric.{Metric, MetricWriter}
import fs2.kafka._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.http4s.client.Client
import org.http4s.client.asynchttpclient.AsyncHttpClient

import scala.concurrent.duration._
import scala.language.postfixOps

object ConsumerLagDetector extends IOApp {
  case class LagInfo(topic: String, partition: Int, lag: Long)
  case class Resources(adminClient: KafkaAdminClient[IO], consumer: KafkaConsumer[IO, Array[Byte], Array[Byte]], client: Client[IO])

  override def run(args: List[String]): IO[ExitCode] = stream.compile.drain.as(ExitCode.Success)

  def stream: Stream[IO, Unit] =
    for {
      config <- Stream.resource(Config.load[IO])
      r <- Stream.resource(resources(config))
      influxWriter = InfluxdbMetricWriter.create(config.influxConfig, r.client)
      id <- Stream.apply(config.consumerGroups.toIndexedSeq:_*)
      _ <- Stream.awakeEvery[IO](60 seconds)
      lagInfo <- Stream.eval(fetchLagInfo(id,r.adminClient, r.consumer))
      _ <- Stream.eval(publish(influxWriter,lagInfo.toList))
    } yield ()


  def resources(config: Config): Resource[IO, Resources] = for {
    adminClient <- adminClientResource(AdminClientSettings[IO]
      .withBootstrapServers(config.bootstrapServers)
      .withClientId("ConsumerLagDetector")
      .withBlocker(Blocker.liftExecutionContext(scala.concurrent.ExecutionContext.Implicits.global)))
    consumer <- consumerResource[IO].using(ConsumerSettings[IO, Array[Byte], Array[Byte]].withBootstrapServers(config.bootstrapServers))
    client <- AsyncHttpClient.resource[IO]()
  } yield Resources(adminClient, consumer, client)

  def fetchLagInfo(consumerGroupId: String, adminClient: KafkaAdminClient[IO], consumer: KafkaConsumer[IO, Array[Byte], Array[Byte]]): IO[Seq[LagInfo]] =
    for {
      offsetInfo <- adminClient.listConsumerGroupOffsets(consumerGroupId).partitionsToOffsetAndMetadata
      endOffsets <- consumer.endOffsets(offsetInfo.toList.map(_._1).toSet)
    } yield merge(offsetInfo,endOffsets)

  def merge(offsetInfo: Map[TopicPartition, OffsetAndMetadata], endOffsets: Map[TopicPartition, Long]): List[LagInfo] =
    offsetInfo.map {
      case (topicPartition, offsetAndMetadata) => LagInfo(topicPartition.topic(), topicPartition.partition(), endOffsets.get(topicPartition).map(_ - offsetAndMetadata.offset()).getOrElse(0L))
    }.toList

  def publish(influxWriter: MetricWriter[IO], info: List[LagInfo]) : IO[Unit] = for {
    time <- timer.clock.realTime(TimeUnit.MILLISECONDS)
    _ <- IO(println(s"Writing ${info.size} measurements"))
    metrics = info.map(li => Metric(
      "lag",
      Map("topic" -> li.topic, "partition" -> li.partition.toString),
      Map("lag" -> li.lag),
      Instant.ofEpochMilli(time)) )
    _ <- influxWriter.write(metrics)
  } yield ()
}
