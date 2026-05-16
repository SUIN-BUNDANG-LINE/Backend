package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.SurveyResponsePayload
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedAutoCloseConsumedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// survey-response-submitted 토픽을 auto-close groupId로 구독하는 Kafka 어댑터.
@Component
class SurveyResponseSubmittedAutoCloseKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(
        topics = ["survey-response-submitted"],
        groupId = "auto-close",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, SurveyResponsePayload::class.java)
        applicationEventPublisher.publishEvent(
            SurveyResponseSubmittedAutoCloseConsumedEvent(
                surveyId = event.surveyId,
                currentParticipantCount = event.currentParticipantCount,
                targetParticipantCount = event.targetParticipantCount,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
