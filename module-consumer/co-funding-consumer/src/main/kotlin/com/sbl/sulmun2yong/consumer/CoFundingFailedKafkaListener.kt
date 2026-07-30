package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedConsumedEvent
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedEvent
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// co-funding-failed 토픽을 co-funding-refund groupId로 구독하는 Kafka 어댑터.
// 책임: payload 역직렬화 -> ApplicationEvent 발행 -> Ack 위임. 환불 팬아웃은 도메인 listener가 수행한다.
@Component
class CoFundingFailedKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(topics = ["co-funding-failed"], groupId = "co-funding-refund")
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingFailedEvent::class.java)
        MDC.put("correlationId", event.eventId)
        applicationEventPublisher.publishEvent(
            CoFundingFailedConsumedEvent(
                eventId = event.eventId,
                fundingId = event.fundingId,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
