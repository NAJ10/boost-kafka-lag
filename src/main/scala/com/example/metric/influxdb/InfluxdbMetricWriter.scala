package com.example.metric.influxdb

import cats.effect.Sync
import com.example.metric.{Metric, MetricWriter}
import org.http4s.{BasicCredentials, Request, Uri}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.io.POST
import org.http4s.headers.Authorization

object InfluxdbMetricWriter {
  def create[F[_] : Sync](config: InfluxConfig, client: Client[F]): MetricWriter[F] = new MetricWriter[F] with Http4sClientDsl[F] {
    val uri = Uri.unsafeFromString(s"${config.scheme}://${config.host}:${config.port}/write?db=${config.db}&precision=n")
    // https://docs.influxdata.com/influxdb/v1.7/administration/authentication_and_authorization/
    val authHeader = Authorization(BasicCredentials(config.username, config.password))

    def write(metrics: Seq[Metric]): F[Unit] = client.expect[Unit](request(metrics))

    def request(metrics: Seq[Metric]): F[Request[F]] =
      POST(
        metrics.map(toLineProtocol).mkString("\n"),
        uri,
        authHeader
      )
  }

  def lineProtocolValue(v: Any) = v match {
    case x : String => s""""$x""""
    case i : Int => s"${i}i"
    case l : Long => s"${l}i"
    case d : Double => s"${d.toString}"
    case f : Float => s"${f.toString}"
  }

  // Documented at https://docs.influxdata.com/influxdb/v1.7/write_protocols/line_protocol_tutorial/
  // but basically it is measurement,tags fields timestamp
  // tags are automatically indexed. fields cannot be indexed.
  def toLineProtocol(metric: Metric) : String = {
    val tags = metric.tags.toList.map(p => s",${p._1}=${p._2}").mkString
    val fields = metric.fields.toList.map(p => s"""${p._1}=${lineProtocolValue(p._2)}""").mkString(",")
    val ts = metric.timestamp.getEpochSecond.toString + "%09d".format(metric.timestamp.getNano)
    s"""${metric.measurement}${tags} ${fields} ${ts}"""
  }
}
