package com.sbl.sulmun2yong.survey.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingCreatedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// ② co-funding-created 구독 - 설문 서비스의 리스너. surveys 는 설문만 쓴다(단일 기록자).
// 개설 tx 가 하던 survey.awaitPayment() 직접 쓰기를 대체한다.
// 재전달 안전: NOT_STARTED 일 때만 전이하는 가드가 멱등을 보장한다.
@Component
class CoFundingCreatedSurveyListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingCreatedSurveyListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_CREATED],
        groupId = "survey-cofunding-created",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingCreatedEvent::class.java)
        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalse(UUID.fromString(event.surveyId))
                .orElse(null)

        when {
            survey == null ->
                log.warn("개설 이벤트의 설문 없음(무시): surveyId={}", event.surveyId)
            survey.status == SurveyStatus.NOT_STARTED ->
                surveyRepository.save(survey.awaitPayment())
            else ->
                log.debug("이미 전이된 설문(멱등 스킵): surveyId={}, status={}", event.surveyId, survey.status)
        }
        ack.acknowledge()
    }
}
