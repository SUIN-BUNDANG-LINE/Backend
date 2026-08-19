package com.sbl.sulmun2yong.global.kafka.consumer

import com.fasterxml.jackson.core.JacksonException
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries

@Configuration
class KafkaDltConfig {
    @Bean
    fun kafkaDltErrorHandler(kafkaTemplate: KafkaTemplate<Any, Any>): DefaultErrorHandler {
        val recoverer =
            DeadLetterPublishingRecoverer(kafkaTemplate) { _, _ ->
                TopicPartition(KafkaTopics.SAGA_DLT, -1)
            }
        val backOff =
            ExponentialBackOffWithMaxRetries(3).apply {
                initialInterval = 1000L
                multiplier = 2.0
            }
        return DefaultErrorHandler(recoverer, backOff).apply {
            addNotRetryableExceptions(JacksonException::class.java)
        }
    }
}
