package com.sbl.sulmun2yong.survey.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingConfirmedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// ⑤ co-funding-confirmed 구독 - 설문 서비스의 모금 활성화 리스너. surveys 는 설문만 쓴다(단일 기록자).
// 장벽 CAS 승자(모금 ④ 리스너)가 설문을 직접 활성화하던 것을 대체한다.
// 재전달 안전: PENDING_PAYMENT 일 때만 여는 가드가 멱등을 보장한다.
@Component
class CoFundingConfirmedSurveyListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingConfirmedSurveyListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_CONFIRMED],
        groupId = "survey-cofunding-confirmed",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingConfirmedEvent::class.java)
        val survey =
            surveyRepository
                .findByIdAndIsDeletedFalse(UUID.fromString(event.surveyId))
                .orElse(null)

        when {
            survey == null ->
                log.warn("개설 확정 이벤트의 설문 없음(무시): surveyId={}", event.surveyId)
            survey.status == SurveyStatus.PENDING_PAYMENT -> {
                surveyRepository.save(survey.start())
                log.info("공동 모금 개설 확정 - 설문 활성화: fundingId={}, surveyId={}", event.fundingId, event.surveyId)
            }
            else ->
                log.debug("활성화 대상 아님(멱등 스킵): surveyId={}, status={}", event.surveyId, survey.status)
        }
        ack.acknowledge()
    }
}
