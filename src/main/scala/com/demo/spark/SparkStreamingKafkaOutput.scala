package com.demo.spark

import java.util

import kafka.serializer.StringDecoder
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

object SparkStreamingKafkaOutput {
  def main(args: Array[String]): Unit = {
    val Array(brokers, topics) = args
    val sparkConf = new SparkConf().setAppName("Spark Streaming Kafka Output")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val topicSet = topics.split(",").toSet
    val kafkaParams = Map[String, String](
      "metadata.broker.list" -> brokers
    )
    // create DStream
    val messages = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](
      ssc, kafkaParams, topicSet)

    val msgCounts = messages.map(_._2).map((v: String) => (v.length, 1)).reduce((v1, v2) => (v1._1 + v2._1, v1._2 + v2._2))
    msgCounts.print()

    msgCounts.foreachRDD(rdd => {
      rdd.foreachPartition(partition => {
        // Print statements in this section are shown in the executor's stdout logs
        val kafkaOpTopic = "test-output"
        val props = new util.HashMap[String, Object]()
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          "org.apache.kafka.common.serialization.StringSerializer")

        val producer = new KafkaProducer[String, String](props)
        partition.foreach(record => {
          val data = record.toString
          // As as debugging technique, users can write to DBFS to verify that records are being written out
          // dbutils.fs.put("/tmp/test_kafka_output",data,true)
          val message = new ProducerRecord[String, String](kafkaOpTopic, null, data)
          producer.send(message)
        })
        producer.close()
      })

    })

    ssc.start()
    ssc.awaitTermination()
  }
}