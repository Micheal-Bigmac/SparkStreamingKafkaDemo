package com.demo.spark

import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

object SparkStreamingKafka {
  def main(args: Array[String]): Unit = {
    val Array(brokers, topics) = args
    val sparkConf = new SparkConf().setAppName("Spark Streaming Kafka")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val topicSet = topics.split(",").toSet
    val kafkaParams = Map[String, String](
      "metadata.broker.list" -> brokers
    )
    // create DStream
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicSet)

    val wordCounts = messages.map(_._2).flatMap(_.split(" ")).map((_, 1L)).reduceByKey(_ + _)
    wordCounts.print()

    ssc.start()
    ssc.awaitTermination()
  }
}