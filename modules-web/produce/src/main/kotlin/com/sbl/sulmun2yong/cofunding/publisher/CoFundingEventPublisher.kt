package com.sbl.sulmun2yong.cofunding.publisher

import com.sbl.sulmun2yong.cofunding.dto.event.CoPaymentSettledEvent
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

// cofunding 도메인의 Kafka 발행 단일 진입점 - Outbox 적재(도메인 변경과 한 트랜잭션) + 커밋 후 즉시 발행 시도.
@Component
class CoFundingEventPublisher(
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    // 참여자 결제 확정 - settle 트랜잭션 안에서 호출되어 SETTLED 전이와 함께 커밋된다.
    // aggregateId = fundingId -> kafkaKey 가 되어 같은 모금의 이벤트가 한 파티션에 직렬화된다(D3)
    fun publishSettled(
        funding: CoFunding,
        participant: CoFundingParticipant,
    ) {
        val event =
            CoPaymentSettledEvent(
                eventId = UUID.randomUUID().toString(),
                fundingId = funding.id.toString(),
                surveyId = funding.surveyId.toString(),
                participantId = participant.id.toString(),
                orderId = participant.tossOrderId,
                settledAt = Instant.now(),
            )

        val outboxEvent =
            outboxEventFactory.create(
                aggregateType = "CoFunding",
                aggregateId = funding.id.toString(),
                eventType = "CoPaymentSettled",
                kafkaTopic = KafkaTopics.CO_PAYMENT_SETTLED,
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
