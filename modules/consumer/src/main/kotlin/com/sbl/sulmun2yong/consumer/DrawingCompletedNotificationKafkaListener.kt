package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedNotificationConsumedEvent
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// drawing-completed 토픽을 drawing-notification groupId로 구독하는 Kafka 어댑터.
// 책임: payload 역직렬화 → ApplicationEvent 발행 → Ack 위임. 도메인 분기/저장은 도메인 listener가 수행한다.
@Component
class DrawingCompletedNotificationKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(
        topics = ["drawing-completed"],
        groupId = "drawing-notification",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, DrawingCompletedPayload::class.java)
        applicationEventPublisher.publishEvent(
            DrawingCompletedNotificationConsumedEvent(
                eventId = event.eventId,
                surveyId = event.surveyId,
                isWinner = event.isWinner,
                rawPayload = payload,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
