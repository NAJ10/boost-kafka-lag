package com.example

import cats.effect.{Resource, Sync}
import com.example.metric.influxdb.InfluxConfig

case class Config(
  bootstrapServers: String,
  consumerGroups: List[String],
  influxConfig: InfluxConfig
)

object Config {
  def load[F[_] : Sync] : Resource[F,Config] = Resource.make(Sync[F].delay {
    Config(
      sys.env.get("BOOTSTRAP_SERVERS").getOrElse("localhost:9092"),
      sys.env("CONSUMER_GROUPS").split(',').toList,
      InfluxConfig(
        sys.env("INFLUX_PROTOCOL"),
        sys.env("INFLUX_HOST"),
        sys.env("INFLUX_PORT").toInt,
        sys.env("INFLUX_DB"),
        sys.env("INFLUX_USER"),
        sys.env("INFLUX_PASSWORD")
      ))
  })( _ => Sync[F].delay(()))

}
