package com.example.metric.influxdb

case class InfluxConfig(
  scheme: String,
  host: String,
  port: Int,
  db: String,
  username: String,
  password: String)
