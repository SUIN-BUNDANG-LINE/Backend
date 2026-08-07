package com.sbl.sulmun2yong.survey.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.event.PaymentSettledEvent
import com.sbl.sulmun2yong.payment.entity.PaymentOrderOrigin
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// ④ payment-settled 구독 - 설문 서비스의 단독 결제 활성화 리스너. surveys 는 설문만 쓴다(단일 기록자).
// settleApproved 가 하던 survey.start() 직접 쓰기를 대체한다.
// 모금 결제 건은 스킵 - 이벤트에 실려 온 origin(주문 발급 출처)으로 판별한다(교차 읽기 없음).
// 활성화는 전원 완료 장벽 통과(⑤ co-funding-confirmed)가 담당한다.
// 재전달 안전: PENDING_PAYMENT 일 때만 여는 가드가 멱등을 보장한다.
@Component
class PaymentSettledSurveyListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentSettledSurveyListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.PAYMENT_SETTLED],
        groupId = "survey-payment-settled",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, PaymentSettledEvent::class.java)
        val surveyId = UUID.fromString(event.surveyId)

        if (event.origin != PaymentOrderOrigin.SOLO.name) {
            log.debug("모금 결제 건 - 단독 활성화 스킵(⑤ 몫): orderId={}", event.orderId)
        } else {
            val survey = surveyRepository.findByIdAndIsDeletedFalse(surveyId).orElse(null)
            when {
                survey == null ->
                    log.warn("정산 이벤트의 설문 없음(무시): surveyId={}", event.surveyId)
                survey.status == SurveyStatus.PENDING_PAYMENT ->
                    surveyRepository.save(survey.start())
                else ->
                    log.debug("이미 전이된 설문(멱등 스킵): surveyId={}, status={}", event.surveyId, survey.status)
            }
        }
        ack.acknowledge()
    }
}
