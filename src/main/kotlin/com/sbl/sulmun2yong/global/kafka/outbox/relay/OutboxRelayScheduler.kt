package com.sbl.sulmun2yong.global.kafka.outbox.relay

import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.metrics.OutboxMetrics
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.TimeUnit

@Component
class OutboxRelayScheduler(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val outboxMetrics: OutboxMetrics,
) {
    companion object {
        private val log = LoggerFactory.getLogger(OutboxRelayScheduler::class.java)
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun relayPendingEvents() {
        outboxEventRepository
            .findPendingForUpdateSkipLocked(
                OutboxStatus.PENDING,
                Instant.now().minusSeconds(60),
                PageRequest.of(0, 10),
            ).forEach { event ->
                val sample = outboxMetrics.startSample()
                try {
                    kafkaEventPublisher
                        .publish(
                            event.kafkaTopic,
                            event.kafkaKey,
                            event.kafkaPayload,
                        ).get(35, TimeUnit.SECONDS)
                    event.markPublished()
                    outboxMetrics.recordPublish(event.kafkaTopic, "success", sample)
                } catch (e: Exception) {
                    log.error("Outbox relay 발행 실패: eventId={}", event.id, e)
                    if (event.incrementRetry()) {
                        log.warn("FAILED 상태 전환됨, outboxId: {}", event.id)
                    }
                    outboxMetrics.recordPublish(event.kafkaTopic, "failure", sample)
                }
            }
    }
}
