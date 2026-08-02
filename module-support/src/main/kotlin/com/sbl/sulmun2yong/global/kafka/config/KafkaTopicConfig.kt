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
    fun drawingCompletedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.DRAWING_COMPLETED)
            .partitions(6)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun drawingNotificationDltTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.DRAWING_NOTIFICATION_DLT)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun coPaymentSettledTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.CO_PAYMENT_SETTLED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun coFundingFailedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.CO_FUNDING_FAILED)
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
            .name(KafkaTopics.PAYMENT_SETTLED)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun paymentFailedTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.PAYMENT_FAILED)
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

    // 사가 토픽별 죽은 편지 큐 - KafkaDltConfig 의 에러핸들러가 "<원본 토픽>.DLT" 로 재발행한다.
    // 기본 resolver 가 같은 파티션 번호로 보내므로 원본과 동일한 3파티션을 유지해야 한다.
    @Bean
    fun sagaDltTopics(): KafkaAdmin.NewTopics =
        KafkaAdmin.NewTopics(
            *listOf(
                KafkaTopics.CO_FUNDING_CREATED,
                KafkaTopics.CO_FUNDING_CONFIRMED,
                KafkaTopics.CO_FUNDING_FAILED,
                KafkaTopics.PAYMENT_SETTLED,
                KafkaTopics.PAYMENT_FAILED,
                KafkaTopics.PAYMENT_REFUNDED,
                KafkaTopics.PAYMENT_CANCEL_REQUESTED,
            ).map { topic ->
                TopicBuilder
                    .name("$topic.DLT")
                    .partitions(3)
                    .replicas(3)
                    .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                    .build()
            }.toTypedArray(),
        )
}
