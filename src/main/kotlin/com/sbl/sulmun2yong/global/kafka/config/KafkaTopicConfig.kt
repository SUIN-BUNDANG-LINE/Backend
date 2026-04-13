package com.sbl.sulmun2yong.global.kafka.config

import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {
    @Bean
    fun surveyResponseSubmittedTopic(): NewTopic =
        TopicBuilder
            .name("survey-response-submitted")
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun drawingCompletedTopic(): NewTopic =
        TopicBuilder
            .name("drawing-completed")
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()

    @Bean
    fun drawingNotificationDltTopic(): NewTopic =
        TopicBuilder
            .name("drawing-notification.DLT")
            .partitions(3)
            .replicas(3)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
            .build()
}
