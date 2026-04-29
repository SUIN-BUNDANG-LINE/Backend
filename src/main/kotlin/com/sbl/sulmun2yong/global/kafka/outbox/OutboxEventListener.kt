package com.sbl.sulmun2yong.global.kafka.outbox

import com.sbl.sulmun2yong.global.kafka.outbox.service.OutboxService
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OutboxEventListener(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val outboxService: OutboxService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: OutboxPublishEvent) {
        kafkaEventPublisher
            .publish(
                topic = event.topic,
                key = event.key,
                payload = event.payload,
            ).whenComplete { _, ex ->
                if (ex == null) {
                    outboxService.markPublishedAsync(event.outboxId)
                } else {
                    outboxService.incrementRetryAsync(event.outboxId)
                }
            }
    }
}
