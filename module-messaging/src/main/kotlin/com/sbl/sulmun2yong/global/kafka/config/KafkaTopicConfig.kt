package com.sbl.sulmun2yong.global.kafka.config

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaAdmin

@Configuration
class KafkaTopicConfig {
    @Bean
    fun coFundingExpiredTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.CO_FUNDING_EXPIRED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun coFundingCreatedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.CO_FUNDING_CREATED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun paymentSettledTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.PAYMENT_SUCCEEDED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun paymentRefundedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.PAYMENT_REFUNDED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun paymentCancelRequestedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.PAYMENT_CANCEL_REQUESTED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun coFundingConfirmedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.CO_FUNDING_CONFIRMED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun sagaHandshakeTopics(): KafkaAdmin.NewTopics =
        KafkaAdmin.NewTopics(
            *listOf(
                KafkaTopics.CO_FUNDING_REQUESTED,
                KafkaTopics.CO_FUNDING_REVIEWED,
            ).map { topic ->
                TopicBuilder
                    .name(topic)
                    .partitions(3)
                    .replicas(3)
                    .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                    .build()
            }.toTypedArray(),
        )

    @Bean
    fun sagaDltTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.SAGA_DLT)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()
}
