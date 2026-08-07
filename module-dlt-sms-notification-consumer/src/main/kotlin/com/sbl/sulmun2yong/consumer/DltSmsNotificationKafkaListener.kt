package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// drawing-notification.DLT 토픽을 dlt-sms-notification groupId로 구독하는 Kafka 어댑터.
@Component
class DltSmsNotificationKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(topics = ["drawing-notification.DLT"], groupId = "dlt-sms-notification")
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, DltSmsNotificationEvent::class.java)
        applicationEventPublisher.publishEvent(DltSmsNotificationConsumedEvent(event))
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
