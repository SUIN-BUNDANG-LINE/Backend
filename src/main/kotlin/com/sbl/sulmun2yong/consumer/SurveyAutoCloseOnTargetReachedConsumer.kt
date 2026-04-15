package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.consumer.payload.SurveyResponsePayload
import com.sbl.sulmun2yong.consumer.repository.SurveyConsumerRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.exception.SurveyNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class SurveyAutoCloseOnTargetReachedConsumer(
    private val surveyConsumerRepository: SurveyConsumerRepository,
    private val objectMapper: ObjectMapper,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private val log =
            LoggerFactory.getLogger(SurveyAutoCloseOnTargetReachedConsumer::class.java)
    }

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

        if (event.targetParticipantCount == null || event.currentParticipantCount < event.targetParticipantCount) {
            applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
            return
        }

        val survey =
            surveyConsumerRepository
                .findByIdAndIsDeletedFalseWithLock(UUID.fromString(event.surveyId))
                .orElseThrow { SurveyNotFoundException() }

        if (survey.status == SurveyStatus.CLOSED) {
            log.info("이미 종료된 설문, surveyId: {}", survey.id)
            applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
            return
        }

        surveyConsumerRepository.save(survey.finish())
        log.info(
            "targetParticipantCount에 도달하여 설문을 종료합니다, 목표 참여자 수: {}, surveyId: {}",
            event.targetParticipantCount,
            survey.id,
        )

        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
