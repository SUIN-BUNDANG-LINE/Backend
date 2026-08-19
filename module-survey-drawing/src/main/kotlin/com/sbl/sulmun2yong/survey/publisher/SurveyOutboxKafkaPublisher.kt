package com.sbl.sulmun2yong.survey.publisher

import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingReviewedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.KafkaRecordOutboxPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class SurveyOutboxKafkaPublisher(
    private val kafkaRecordOutboxPublisher: KafkaRecordOutboxPublisher,
) {
    fun publishApprovedReview(
        fundingId: String,
        surveyId: String,
        boardId: String,
        totalAmount: Int,
    ) = publishReviewed(
        fundingId = fundingId,
        surveyId = surveyId,
        verdict = CoFundingReviewedEvent.Verdict.APPROVED,
        boardId = boardId,
        totalAmount = totalAmount,
    )

    fun publishRejectedReview(
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
        boardId: String? = null,
        totalAmount: Int? = null,
        reason: String? = null,
    ) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.CO_FUNDING_REVIEWED,
            kafkaRecordKey = fundingId,
            kafkaRecordValue =
                CoFundingReviewedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = fundingId,
                    surveyId = surveyId,
                    verdict = verdict,
                    boardId = boardId,
                    totalAmount = totalAmount,
                    reason = reason,
                    reviewedAt = Instant.now(),
                ),
        )
    }

}
