package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.SurveyResponsePayload
import com.sbl.sulmun2yong.global.kafka.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.survey.dto.event.SurveyResponseSubmittedStatsConsumedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

// survey-response-submitted 토픽을 response-stats groupId로 fan-out 구독하는 Kafka 어댑터.
// auto-close 그룹과 동일한 토픽을 별도 groupId로 받아 응답 통계 집계 listener에 전달한다.
@Component
class SurveyResponseSubmittedStatsKafkaListener(
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @KafkaListener(
        topics = ["survey-response-submitted"],
        groupId = "response-stats",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, SurveyResponsePayload::class.java)
        applicationEventPublisher.publishEvent(
            SurveyResponseSubmittedStatsConsumedEvent(
                surveyId = event.surveyId,
                currentParticipantCount = event.currentParticipantCount,
            ),
        )
        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
