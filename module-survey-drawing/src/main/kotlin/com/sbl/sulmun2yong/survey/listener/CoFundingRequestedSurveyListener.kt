package com.sbl.sulmun2yong.survey.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingRequestedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.domain.reward.ImmediateDrawSetting
import com.sbl.sulmun2yong.survey.publisher.SurveySagaPublisher
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// co-funding-requested 구독 - 설문 서비스의 개설 판정 리스너. surveys 는 설문만 쓴다(단일 기록자).
// 판정 권한이 데이터 소유자(설문)에 있다 - 소유자·상태·경품 설정을 자기 트랜잭션에서 검증하므로
// "검증 통과 직후 상태 변경" 경합(TOCTOU)이 없다. 승인이면 PENDING_PAYMENT 전이 + 총액 확정을
// 한 트랜잭션으로 커밋하고 approved 를, 아니면 rejected 를 회신한다.
// 재전달 안전: 이미 PENDING_PAYMENT 면 approved 재발행 - 모금 쪽 PENDING_APPROVAL 가드가 흡수한다.
@Component
class CoFundingRequestedSurveyListener(
    private val objectMapper: ObjectMapper,
    private val surveyRepository: SurveyRepository,
    private val sagaPublisher: SurveySagaPublisher,
    @Value("\${payment.reward-unit-price}")
    private val rewardUnitPrice: Int,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingRequestedSurveyListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_REQUESTED],
        groupId = "survey-cofunding-requested",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingRequestedEvent::class.java)
        val survey =
            surveyRepository
                .findByIdAndMakerIdAndIsDeletedFalse(
                    UUID.fromString(event.surveyId),
                    UUID.fromString(event.ownerId),
                ).orElse(null)

        val rewardSetting = survey?.rewardSetting
        when {
            survey == null ->
                reject(event, "설문이 없거나 개설자 소유가 아님")
            rewardSetting !is ImmediateDrawSetting ->
                reject(event, "경품(즉시 추첨) 설문이 아님")
            survey.status == SurveyStatus.NOT_STARTED -> {
                surveyRepository.save(survey.awaitPayment())
                approve(event, rewardUnitPrice * rewardSetting.rewards.sumOf { it.count })
            }
            survey.status == SurveyStatus.PENDING_PAYMENT ->
                // 재전달 - 전이는 이미 됐으니 승인만 재회신(모금 가드가 멱등 흡수)
                approve(event, rewardUnitPrice * rewardSetting.rewards.sumOf { it.count })
            else ->
                reject(event, "이미 시작·종료된 설문 (status=${survey.status})")
        }
        ack.acknowledge()
    }

    private fun approve(
        event: CoFundingRequestedEvent,
        totalAmount: Int,
    ) {
        sagaPublisher.publishApproved(event.fundingId, event.surveyId, totalAmount)
        log.info("모금 개설 승인: fundingId={}, totalAmount={}", event.fundingId, totalAmount)
    }

    private fun reject(
        event: CoFundingRequestedEvent,
        reason: String,
    ) {
        sagaPublisher.publishRejected(event.fundingId, event.surveyId, reason)
        log.info("모금 개설 거절: fundingId={}, reason={}", event.fundingId, reason)
    }
}
