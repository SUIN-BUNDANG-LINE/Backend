package com.sbl.sulmun2yong.survey.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.event.PaymentFailedEvent
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// payment-failed 구독 - 설문 서비스의 단독 결제 복귀 리스너. surveys 는 설문만 쓴다(단일 기록자).
// settleRejected·handleFail 이 하던 survey.revertToNotStarted() 직접 쓰기를 대체한다.
// 모금 걸린 설문은 스킵 - 모금은 FUNDING 유지, 거절 참여자는 기한 만료 무산 경로(교차 읽기로 판별).
// 재전달 안전: PENDING_PAYMENT 일 때만 되돌리는 가드가 멱등을 보장한다.
@Component
class PaymentFailedSurveyListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
    private val coFundingRepository: CoFundingRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentFailedSurveyListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.PAYMENT_FAILED],
        groupId = "survey-payment-failed",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, PaymentFailedEvent::class.java)
        val surveyId = UUID.fromString(event.surveyId)

        if (coFundingRepository.findBySurveyId(surveyId) != null) {
            log.debug("모금 결제 건 - 설문 복귀 스킵(기한 만료 무산 경로): orderId={}", event.orderId)
        } else {
            val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElse(null)
            when {
                survey == null ->
                    log.warn("실패 이벤트의 설문 없음(무시): surveyId={}", event.surveyId)
                survey.status == SurveyStatus.PENDING_PAYMENT ->
                    surveyRepository.save(survey.revertToNotStarted())
                else ->
                    log.debug("복귀 대상 아님(멱등 스킵): surveyId={}, status={}", event.surveyId, survey.status)
            }
        }
        ack.acknowledge()
    }
}
