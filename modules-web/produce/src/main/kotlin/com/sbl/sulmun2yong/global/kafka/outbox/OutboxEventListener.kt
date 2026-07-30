package com.sbl.sulmun2yong.global.kafka.outbox

import com.sbl.sulmun2yong.global.kafka.outbox.metrics.OutboxMetrics
import com.sbl.sulmun2yong.global.kafka.outbox.service.OutboxService
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OutboxEventListener(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val outboxService: OutboxService,
    private val outboxMetrics: OutboxMetrics,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("outboxAsyncExecutor")
    fun handle(event: OutboxPublishEvent) {
        val sample = outboxMetrics.startSample()

        kafkaEventPublisher
            .publish(
                topic = event.topic,
                key = event.key,
                payload = event.payload,
            ).whenComplete { _, ex ->
                if (ex == null) {
                    outboxService.markPublishedAsync(event.outboxId)
                    outboxMetrics.recordPublish(event.topic, "success", sample)
                } else {
                    outboxService.incrementRetryAsync(event.outboxId)
                    outboxMetrics.recordPublish(event.topic, "failure", sample)
                }
            }
    }
}
