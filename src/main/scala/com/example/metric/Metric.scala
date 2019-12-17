package com.example.metric

import java.time.Instant

/**
 * Models data that gets written to influxdb
 * @param measurement
 * @param tags
 * @param fields
 * @param timestamp
 */
case class Metric(
  measurement: String,
  tags: Map[String, String], // Tags should not have high cardinality. tag values have to be strings
  fields: Map[String, Any],  // fields are not indexable. Should not be used in queries
  timestamp: Instant
)
