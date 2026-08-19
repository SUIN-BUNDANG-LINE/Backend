package com.sbl.sulmun2yong.cofunding.publisher

import com.sbl.sulmun2yong.cofunding.dto.event.*
import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.KafkaRecordOutboxPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class CoFundingOutboxKafkaPublisher(
    private val kafkaRecordOutboxPublisher: KafkaRecordOutboxPublisher,
) {
    fun publishRequested(funding: CoFunding) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.CO_FUNDING_REQUESTED,
            kafkaRecordKey = funding.id.toString(),
            kafkaRecordValue =
                CoFundingRequestedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    surveyId = funding.surveyId.toString(),
                    ownerId = funding.ownerId.toString(),
                    requestedAt = Instant.now(),
                ),
        )
    }

    fun publishCreated(
        funding: CoFunding,
        participants: List<CoFundingParticipant>,
        boardId: String,
    ) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.CO_FUNDING_CREATED,
            kafkaRecordKey = funding.id.toString(),
            kafkaRecordValue =
                CoFundingCreatedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    boardId = boardId,
                    participants =
                        participants.map {
                            CoFundingCreatedEvent.Participant(
                                userId = it.userId.toString(),
                                orderId = it.tossOrderId,
                                amount = funding.shareAmount,
                            )
                        },
                    createdAt = Instant.now(),
                ),
        )
    }

    fun publishExpired(
        funding: CoFunding,
        paidOrderIds: List<String>,
    ) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.CO_FUNDING_EXPIRED,
            kafkaRecordKey = funding.id.toString(),
            kafkaRecordValue =
                CoFundingExpiredEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    surveyId = funding.surveyId.toString(),
                    paidOrderIds = paidOrderIds,
                    expiredAt = Instant.now(),
                ),
        )
    }

    fun publishConfirmed(funding: CoFunding) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.CO_FUNDING_CONFIRMED,
            kafkaRecordKey = funding.id.toString(),
            kafkaRecordValue =
                CoFundingConfirmedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    surveyId = funding.surveyId.toString(),
                    confirmedAt = Instant.now(),
                ),
        )
    }

    fun publishCancelRequested(orderId: String) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.PAYMENT_CANCEL_REQUESTED,
            kafkaRecordKey = orderId,
            kafkaRecordValue =
                PaymentCancelRequestedEvent(
                    eventId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    requestedAt = Instant.now(),
                ),
        )
    }
}
