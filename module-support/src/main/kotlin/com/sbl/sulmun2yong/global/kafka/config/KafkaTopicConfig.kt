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

    // 개설 판정 사가(요청·판정 결과)와 단독 개시 이벤트 - 전 교차 접근의 이벤트화로 신설.
    @Bean
    fun sagaHandshakeTopics(): KafkaAdmin.NewTopics =
        KafkaAdmin.NewTopics(
            *listOf(
                KafkaTopics.CO_FUNDING_REQUESTED,
                KafkaTopics.CO_FUNDING_REVIEWED,
                KafkaTopics.SURVEY_PAYMENT_PENDING,
            ).map { topic ->
                TopicBuilder
                    .name(topic)
                    .partitions(3)
                    .replicas(3)
                    .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                    .build()
            }.toTypedArray(),
        )

    // 사가 리스너 실패의 통합 죽은 편지 큐 - KafkaDltConfig 의 에러핸들러가 여기로 재발행한다.
    // 토픽별로 쪼개지 않는다 - 원본 토픽은 kafka_dlt-original-topic 헤더가 보존한다.
    @Bean
    fun sagaDltTopic(): NewTopic =
        TopicBuilder
            .name(KafkaTopics.SAGA_DLT)
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()
}
