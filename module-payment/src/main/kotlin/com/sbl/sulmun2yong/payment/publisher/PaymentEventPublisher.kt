package com.sbl.sulmun2yong.payment.publisher

import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxEventFactory
import com.sbl.sulmun2yong.global.kafka.outbox.OutboxPublishEvent
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.payment.dto.event.PaymentFailedEvent
import com.sbl.sulmun2yong.payment.dto.event.PaymentRefundedEvent
import com.sbl.sulmun2yong.payment.dto.event.PaymentSettledEvent
import com.sbl.sulmun2yong.payment.entity.PaymentOrder
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

// payment 도메인의 Kafka 발행 단일 진입점 - Outbox 적재(정산 tx 와 한 트랜잭션) + 커밋 후 즉시 발행 시도.
// 결제는 사실만 발행한다 - 설문 전이 등 남의 상태 변경은 구독자 몫(단일 기록자).
@Component
class PaymentEventPublisher(
    private val outboxEventFactory: OutboxEventFactory,
    private val outboxEventRepository: OutboxEventRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    // confirm 성공 사실 - settleApproved tx 안에서 호출되어 장부 DONE 과 함께 커밋된다(단독·모금 불문).
    fun publishSettled(order: PaymentOrder) {
        publish(
            eventType = "PaymentSettled",
            kafkaTopic = KafkaTopics.PAYMENT_SETTLED,
            order = order,
            event =
                PaymentSettledEvent(
                    eventId = UUID.randomUUID().toString(),
                    orderId = order.tossOrderId,
                    surveyId = order.surveyId.toString(),
                    origin = order.origin.name,
                    settledAt = Instant.now(),
                ),
        )
    }

    // confirm 거절·결제창 이탈 사실 - settleRejected·handleFail tx 안에서 호출된다.
    fun publishFailed(order: PaymentOrder) {
        publish(
            eventType = "PaymentFailed",
            kafkaTopic = KafkaTopics.PAYMENT_FAILED,
            order = order,
            event =
                PaymentFailedEvent(
                    eventId = UUID.randomUUID().toString(),
                    orderId = order.tossOrderId,
                    surveyId = order.surveyId.toString(),
                    origin = order.origin.name,
                    failedAt = Instant.now(),
                ),
        )
    }

    // 환불(CANCEL) 완료 사실 - 릴레이의 CANCEL 승인 tx 안에서 호출된다.
    // 참여자 REFUNDED 전이·FAILED→REFUNDED 수렴 CAS 는 모금 ⑦ 리스너 몫(단일 기록자).
    fun publishRefunded(order: PaymentOrder) {
        publish(
            eventType = "PaymentRefunded",
            kafkaTopic = KafkaTopics.PAYMENT_REFUNDED,
            order = order,
            event =
                PaymentRefundedEvent(
                    eventId = UUID.randomUUID().toString(),
                    orderId = order.tossOrderId,
                    surveyId = order.surveyId.toString(),
                    refundedAt = Instant.now(),
                ),
        )
    }

    private fun publish(
        eventType: String,
        kafkaTopic: String,
        order: PaymentOrder,
        event: Any,
    ) {
        val outboxEvent =
            outboxEventFactory.create(
                aggregateType = "PaymentOrder",
                aggregateId = order.tossOrderId,
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
