package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.notification.dto.event.SagaDltConsumedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// 사가 토픽의 죽은 편지 큐(*.DLT)를 dlt-saga groupId로 구독하는 Kafka 어댑터.
// 발행측(웹·모금·결제)의 KafkaDltConfig 에러핸들러가 재발행한 원본 레코드를 받아,
// kafka_dlt-* 헤더의 실패 정보와 함께 도메인 계층으로 넘긴다. 적재는 SagaDltMessageEventListener 몫.
@Component
class SagaDltKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(
        topics = [
            "co-funding-created.DLT",
            "co-funding-confirmed.DLT",
            "co-funding-failed.DLT",
            "payment-settled.DLT",
            "payment-failed.DLT",
            "payment-refunded.DLT",
            "payment-cancel-requested.DLT",
        ],
        groupId = "dlt-saga",
    )
    @Transactional
    fun handle(
        payload: String,
        @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false) originalTopic: ByteArray?,
        @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) exceptionMessage: ByteArray?,
        ack: Acknowledgment,
    ) {
        // 사가 이벤트는 모두 eventId 를 갖는다 - payload 가 JSON 조차 아니어서 못 꺼내면(그래서 DLT 로 온
        // poison pill) 대체 키를 발급해 적재는 반드시 성사시킨다.
        val eventId =
            runCatching { objectMapper.readTree(payload).path("eventId").asText() }
                .getOrNull()
                .takeUnless { it.isNullOrBlank() }
                ?: UUID.randomUUID().toString()

        applicationEventPublisher.publishEvent(
            SagaDltConsumedEvent(
                eventId = eventId,
                originalTopic = originalTopic?.toString(Charsets.UTF_8) ?: "unknown",
                payload = payload,
                exceptionMessage = exceptionMessage?.toString(Charsets.UTF_8),
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
