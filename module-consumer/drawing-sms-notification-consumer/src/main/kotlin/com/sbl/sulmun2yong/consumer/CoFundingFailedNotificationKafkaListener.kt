package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedNotificationConsumedEvent
import com.sbl.sulmun2yong.consumer.payload.CoFundingFailedPayload
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// co-funding-failed 토픽을 cofunding-failed-notification groupId로 구독하는 Kafka 어댑터.
// 책임: payload 역직렬화 → ApplicationEvent 발행 → Ack 위임. 잡 적재는 도메인 listener가 수행한다.
// 재전달 시 SMS 잡은 UNIQUE(event_id, notification_type)로 중복이 흡수된다.
@Component
class CoFundingFailedNotificationKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    companion object {
        const val TOPIC = "co-funding-failed"
    }

    @KafkaListener(
        topics = [TOPIC],
        groupId = "cofunding-failed-notification",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingFailedPayload::class.java)
        // 상류 상관 헤더가 없을 수 있으므로 payload 의 사가 키로 시드한다.
        MDC.put("correlationId", event.eventId)
        applicationEventPublisher.publishEvent(
            CoFundingFailedNotificationConsumedEvent(
                eventId = event.eventId,
                fundingId = event.fundingId,
                rawPayload = payload,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
