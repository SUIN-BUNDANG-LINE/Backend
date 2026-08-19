package com.sbl.sulmun2yong.payment.publisher

import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.global.kafka.outbox.KafkaRecordOutboxPublisher
import com.sbl.sulmun2yong.payment.dto.event.PaymentRefundedEvent
import com.sbl.sulmun2yong.payment.dto.event.PaymentSucceededEvent
import com.sbl.sulmun2yong.payment.entity.TossOrderEntity
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class PaymentEventPublisher(
    private val kafkaRecordOutboxPublisher: KafkaRecordOutboxPublisher,
) {
    fun publishSucceeded(order: TossOrderEntity) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.PAYMENT_SUCCEEDED,
            kafkaRecordKey = order.id,
            kafkaRecordValue =
                PaymentSucceededEvent(
                    eventId = UUID.randomUUID().toString(),
                    orderId = order.id,
                    productType = order.productType.name,
                    productId = order.productId.toString(),
                    succeededAt = Instant.now(),
                ),
        )
    }

    fun publishRefunded(order: TossOrderEntity) {
        kafkaRecordOutboxPublisher.publish(
            kafkaTopic = KafkaTopics.PAYMENT_REFUNDED,
            kafkaRecordKey = order.id,
            kafkaRecordValue =
                PaymentRefundedEvent(
                    eventId = UUID.randomUUID().toString(),
                    orderId = order.id,
                    productType = order.productType.name,
                    productId = order.productId.toString(),
                    refundedAt = Instant.now(),
                ),
        )
    }
}
