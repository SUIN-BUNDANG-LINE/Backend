package com.sbl.sulmun2yong.cofunding.publisher

import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingConfirmedEvent
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingCreatedEvent
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedEvent
import com.sbl.sulmun2yong.cofunding.dto.event.PaymentCancelRequestedEvent
import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventFactory
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

// 모금 서비스의 Kafka 발행 단일 진입점 - Outbox 적재(도메인 tx 와 한 트랜잭션) + 커밋 후 즉시 발행.
// ② 개설 · ⑤ 장벽 통과 · ⑥ 무산 · ⑧ 늦은 결제 환불 명령 전부 여기서 나간다.
@Component
class CoFundingSagaPublisher(
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    // ② 개설 확정 사실 - 개설 tx 안에서 호출되어 co_fundings·participants 저장과 함께 커밋된다.
    // 주문 발급(결제)·설문 대기 전이(설문)·보드 생성(추첨)은 구독자가 수행한다(단일 기록자).
    fun publishCreated(
        funding: CoFunding,
        participants: List<CoFundingParticipant>,
    ) {
        publish(
            aggregateId = funding.id.toString(),
            eventType = "CoFundingCreated",
            kafkaTopic = KafkaTopics.CO_FUNDING_CREATED,
            event =
                CoFundingCreatedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    surveyId = funding.surveyId.toString(),
                    participants =
                        participants.map {
                            CoFundingCreatedEvent.Participant(
                                userId = it.userId.toString(),
                                orderId = it.tossOrderId,
                                amount = if (it.isOwner) funding.ownerShareAmount else funding.shareAmount,
                            )
                        },
                    createdAt = Instant.now(),
                ),
        )
    }

    // ⑥ 무산 사실 - 기한 스케줄러의 tryFail 승자 tx 안에서 호출된다(Outbox 라 유령 신호·유실 둘 다 없음).
    // settledOrderIds 는 잠금 후 스냅샷 - CANCEL 적재는 결제 ⑥ 리스너 몫.
    fun publishFailed(
        funding: CoFunding,
        settledOrderIds: List<String>,
    ) {
        publish(
            aggregateId = funding.id.toString(),
            eventType = "CoFundingFailed",
            kafkaTopic = KafkaTopics.CO_FUNDING_FAILED,
            event =
                CoFundingFailedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    surveyId = funding.surveyId.toString(),
                    settledOrderIds = settledOrderIds,
                    failedAt = Instant.now(),
                ),
        )
    }

    // ⑤ 전원 SETTLED 장벽 통과 사실 - tryConfirm CAS 승자만 호출한다(모금당 정확히 1회).
    fun publishConfirmed(funding: CoFunding) {
        publish(
            aggregateId = funding.id.toString(),
            eventType = "CoFundingConfirmed",
            kafkaTopic = KafkaTopics.CO_FUNDING_CONFIRMED,
            event =
                CoFundingConfirmedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    surveyId = funding.surveyId.toString(),
                    confirmedAt = Instant.now(),
                ),
        )
    }

    // ⑧ 늦은 결제 단건 환불 명령 - 무산(FAILED·REFUNDED) 후 도착한 결제 확정을 발견했을 때.
    fun publishCancelRequested(orderId: String) {
        publish(
            aggregateId = orderId,
            eventType = "PaymentCancelRequested",
            kafkaTopic = KafkaTopics.PAYMENT_CANCEL_REQUESTED,
            event =
                PaymentCancelRequestedEvent(
                    eventId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    requestedAt = Instant.now(),
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
                aggregateType = "CoFunding",
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
