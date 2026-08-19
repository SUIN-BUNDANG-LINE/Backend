package com.sbl.sulmun2yong.global.kafka.outbox.relay

import com.sbl.sulmun2yong.global.kafka.outbox.metrics.KafkaRecordOutboxMetrics
import com.sbl.sulmun2yong.global.kafka.outbox.service.KafkaRecordOutboxService
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KafkaRecordOutboxRelayScheduler(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val kafkaRecordOutboxService: KafkaRecordOutboxService,
    private val kafkaRecordOutboxMetrics: KafkaRecordOutboxMetrics,
) {
    companion object {
        private const val BATCH_SIZE = 10

        // 즉시 발행(AFTER_COMMIT 리스너)이 아직 발송 중일 수 있는 행을
        // 가로채 이중 발행하지 않기 위한 유예
        private const val STALE_SECONDS = 60L
    }

    @Scheduled(fixedDelay = 5000)
    fun relayPendingEvents() {
        kafkaRecordOutboxService.claimPendingForRelay(BATCH_SIZE, STALE_SECONDS).forEach { event ->
            val sample = kafkaRecordOutboxMetrics.startSample()
            kafkaEventPublisher
                .publish(event.kafkaTopic, event.kafkaRecordKey, event.kafkaRecordValue)
                .whenComplete { _, ex ->
                    if (ex == null) {
                        kafkaRecordOutboxService.markPublishedAsync(event.id)
                        kafkaRecordOutboxMetrics.recordPublish(event.kafkaTopic, "success", sample)
                    } else {
                        kafkaRecordOutboxService.incrementRetryAsync(event.id)
                        kafkaRecordOutboxMetrics.recordPublish(event.kafkaTopic, "failure", sample)
                    }
                }
        }
    }
}
