package com.example.metric

trait MetricWriter[F[_]] {
  def write(seq: Seq[Metric]) : F[Unit]
}
