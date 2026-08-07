package com.sbl.sulmun2yong.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.dto.event.SmsDeliveryPermanentlyFailedEvent
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * [DeliveryPermanentlyFailedDispatcher] 의 Kafka 구현 — 영구 실패 신호를 `sms-delivery-permanently-failed` 토픽으로 발행한다.                                                                                                       *
 * DLT 봉투([DltSmsNotificationEvent])의 `payload` 에는 원본 drawing-completed JSON 이 통째로 들어있다.
 * 이를 [DrawingCompletedPayload] 로 파싱해 상관 키(`originalDrawingEventId`)와 참여자/설문 식별자를 꺼내
 * [SmsDeliveryPermanentlyFailedEvent] 로 조립·직렬화한 뒤 [KafkaEventPublisher] 로 발행한다.
 *
 * 메시지 키는 `originalDrawingEventId` 로 고정해, 같은 당첨 건의 사가 이벤트들이 동일 파티션에 실려
 * 순서/정합성이 유지되도록 한다. notification 모듈의 KafkaDeliveryConfirmedDispatcher 와 대칭 구조다.
 */
@Component
class KafkaDeliveryPermanentlyFailedDispatcher(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val objectMapper: ObjectMapper,
) : DeliveryPermanentlyFailedDispatcher {
    companion object {
        const val FAILED_TOPIC = "sms-delivery-permanently-failed"
    }

    override fun dispatch(event: DltSmsNotificationEvent) {
        val original = objectMapper.readValue(event.payload, DrawingCompletedPayload::class.java)
        val failedEvent =
            SmsDeliveryPermanentlyFailedEvent(
                eventId = UUID.randomUUID().toString(),
                originalDrawingEventId = original.eventId,
                participantId = original.participantId,
                surveyId = original.surveyId,
                errorCode = event.lastError ?: "DLT_UNKNOWN",
                failedAt = event.failedAt,
            )
        kafkaEventPublisher.publish(
            FAILED_TOPIC,
            failedEvent.originalDrawingEventId,
            objectMapper.writeValueAsString(failedEvent),
        )
    }
}
