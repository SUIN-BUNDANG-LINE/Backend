package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.survey.dto.event.DrawingCompletedAutoCloseConsumedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// drawing-completed 토픽을 drawing-auto-close groupId로 구독하는 Kafka 어댑터.
@Component
class DrawingCompletedAutoCloseKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(
        topics = ["drawing-completed"],
        groupId = "drawing-auto-close",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, DrawingCompletedPayload::class.java)
        applicationEventPublisher.publishEvent(
            DrawingCompletedAutoCloseConsumedEvent(
                surveyId = event.surveyId,
                remainingTickets = event.remainingTickets,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
