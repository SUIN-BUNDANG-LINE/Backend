package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoPaymentSettledConsumedEvent
import com.sbl.sulmun2yong.cofunding.dto.event.CoPaymentSettledEvent
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// co-payment-settled 토픽을 co-funding-settlement groupId로 구독하는 Kafka 어댑터.
// 책임: payload 역직렬화 -> ApplicationEvent 발행 -> Ack 위임. 장벽 판정은 도메인 listener가 수행한다.
// 재수신/재전달은 장벽 CAS 패배(no-op)로 무해하다.
@Component
class CoPaymentSettledKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(
        topics = ["co-payment-settled"],
        groupId = "co-funding-settlement",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoPaymentSettledEvent::class.java)
        // 사가 키(eventId)로 상관관계 시드 - 로그/트레이스 추적용
        MDC.put("correlationId", event.eventId)
        applicationEventPublisher.publishEvent(
            CoPaymentSettledConsumedEvent(
                eventId = event.eventId,
                fundingId = event.fundingId,
                surveyId = event.surveyId,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
