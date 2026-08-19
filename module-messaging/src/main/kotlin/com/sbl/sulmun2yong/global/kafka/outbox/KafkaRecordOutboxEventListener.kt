package com.sbl.sulmun2yong.global.kafka.outbox

import com.sbl.sulmun2yong.global.kafka.outbox.metrics.KafkaRecordOutboxMetrics
import com.sbl.sulmun2yong.global.kafka.outbox.service.KafkaRecordOutboxService
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class KafkaRecordOutboxEventListener(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val kafkaRecordOutboxService: KafkaRecordOutboxService,
    private val kafkaRecordOutboxMetrics: KafkaRecordOutboxMetrics,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("outboxAsyncExecutor")
    fun handle(event: KafkaRecordOutboxPublishEvent) {
        val sample = kafkaRecordOutboxMetrics.startSample()

        kafkaEventPublisher
            .publish(
                topic = event.kafkaTopic,
                key = event.kafkaRecordKey,
                value = event.kafkaRecordValue,
            ).whenComplete { _: Any, ex: Throwable? ->
                if (ex == null) {
                    kafkaRecordOutboxService.markPublishedAsync(event.outboxId)
                    kafkaRecordOutboxMetrics.recordPublish(event.kafkaTopic, "success", sample)
                } else {
                    kafkaRecordOutboxService.incrementRetryAsync(event.outboxId)
                    kafkaRecordOutboxMetrics.recordPublish(event.kafkaTopic, "failure", sample)
                }
            }
    }
}
