package com.sbl.sulmun2yong.global.kafka.outbox.relay

import com.sbl.sulmun2yong.global.kafka.outbox.metrics.OutboxMetrics
import com.sbl.sulmun2yong.global.kafka.outbox.service.OutboxService
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxRelayScheduler(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val outboxService: OutboxService,
    private val outboxMetrics: OutboxMetrics,
) {
    companion object {
        private val log = LoggerFactory.getLogger(OutboxRelayScheduler::class.java)
        private const val BATCH_SIZE = 10
    }

    @Scheduled(fixedDelay = 5000)
    fun relayPendingEvents() {
        outboxService.claimPendingForRelay(BATCH_SIZE).forEach { event ->
            val sample = outboxMetrics.startSample()
            kafkaEventPublisher
                .publish(event.kafkaTopic, event.kafkaKey, event.kafkaPayload)
                .whenComplete { _, ex ->
                    if (ex == null) {
                        outboxService.markPublishedAsync(event.id)
                        outboxMetrics.recordPublish(event.kafkaTopic, "success", sample)
                    } else {
                        outboxService.markPublishedAsync(event.id)
                        outboxMetrics.recordPublish(event.kafkaTopic, "failure", sample)
                    }
                }
        }
    }
}
