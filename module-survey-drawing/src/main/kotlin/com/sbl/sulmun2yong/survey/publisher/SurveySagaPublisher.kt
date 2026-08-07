package com.sbl.sulmun2yong.survey.publisher

import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingReviewedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventFactory
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.survey.dto.event.SurveyPaymentPendingEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

// 설문 도메인의 사가 발행 단일 진입점 - Outbox 적재(도메인 tx 와 한 트랜잭션) + 커밋 후 즉시 발행.
// 모금 판정 회신(approved/rejected)과 단독 개시(survey-payment-pending)가 여기서 나간다.
@Component
class SurveySagaPublisher(
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    // 모금 판정 승인 - 판정 tx 안에서 PENDING_PAYMENT 전이·보드 생성과 함께 커밋된다.
    // totalAmount(단가 × 경품 수)는 설문 소유 데이터라 여기서 확정해 실어 보낸다.
    fun publishApproved(
        fundingId: String,
        surveyId: String,
        totalAmount: Int,
    ) = publishReviewed(
        fundingId = fundingId,
        surveyId = surveyId,
        verdict = CoFundingReviewedEvent.Verdict.APPROVED,
        totalAmount = totalAmount,
    )

    // 모금 판정 거절 - 소유자 불일치·상태 부적합 등. 모금은 이 회신으로 REJECTED 종착한다.
    fun publishRejected(
        fundingId: String,
        surveyId: String,
        reason: String,
    ) = publishReviewed(
        fundingId = fundingId,
        surveyId = surveyId,
        verdict = CoFundingReviewedEvent.Verdict.REJECTED,
        reason = reason,
    )

    private fun publishReviewed(
        fundingId: String,
        surveyId: String,
        verdict: CoFundingReviewedEvent.Verdict,
        totalAmount: Int? = null,
        reason: String? = null,
    ) {
        publish(
            aggregateId = fundingId,
            eventType = "CoFundingReviewed",
            kafkaTopic = KafkaTopics.CO_FUNDING_REVIEWED,
            event =
                CoFundingReviewedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = fundingId,
                    surveyId = surveyId,
                    verdict = verdict,
                    totalAmount = totalAmount,
                    reason = reason,
                    reviewedAt = Instant.now(),
                ),
        )
    }

    // 단독 개시 - startSurvey tx 안에서 PENDING_PAYMENT 전이·보드 생성과 함께 커밋된다.
    // 주문 발급(결제, origin=SOLO)은 구독자 몫 - 재호출 시 재발행해도 발급이 멱등이라 안전하다.
    fun publishPaymentPending(
        surveyId: String,
        makerId: String,
        amount: Int,
    ) {
        publish(
            aggregateId = surveyId,
            eventType = "SurveyPaymentPending",
            kafkaTopic = KafkaTopics.SURVEY_PAYMENT_PENDING,
            event =
                SurveyPaymentPendingEvent(
                    eventId = UUID.randomUUID().toString(),
                    surveyId = surveyId,
                    makerId = makerId,
                    amount = amount,
                    occurredAt = Instant.now(),
                ),
        )
    }

    private fun publish(
        aggregateId: String,
        eventType: String,
        kafkaTopic: String,
        event: Any,
    ) {
        val outboxEvent =
            outboxEventFactory.create(
                aggregateType = "Survey",
                aggregateId = aggregateId,
                eventType = eventType,
                kafkaTopic = kafkaTopic,
                event = event,
            )

        outboxEventRepository.save(outboxEvent)

        applicationEventPublisher.publishEvent(
            OutboxPublishEvent(
                outboxId = outboxEvent.id,
                topic = outboxEvent.kafkaTopic,
                key = outboxEvent.kafkaKey,
                payload = outboxEvent.kafkaPayload,
            ),
        )
    }
}
