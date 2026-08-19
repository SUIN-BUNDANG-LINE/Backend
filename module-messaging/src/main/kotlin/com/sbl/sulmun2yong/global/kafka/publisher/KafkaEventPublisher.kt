package com.sbl.sulmun2yong.global.kafka.publisher

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

@Component
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    companion object {
        private val log = LoggerFactory.getLogger(KafkaEventPublisher::class.java)
    }

    fun publish(
        topic: String,
        key: String,
        value: String,
    ): CompletableFuture<SendResult<String, String>> {
        val record = ProducerRecord<String, String>(topic, key, value)

        MDC.get("correlationId")?.let { id ->
            record.headers().add("X-Correlation-Id", id.toByteArray(StandardCharsets.UTF_8))
        }

        return try {
            kafkaTemplate.send(record)
        } catch (e: Exception) {
            CompletableFuture.failedFuture(e)
        }.whenComplete { _: Any, error: Throwable? ->
            if (error == null) {
                log.info("Kafka 발행 성공: topic={}, key={}", topic, key)
            } else {
                log.warn("Kafka 발행 실패: topic={}, key={}, error={}", topic, key, error.message)
            }
        }
    }
}
