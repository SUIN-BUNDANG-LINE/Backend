package com.sbl.sulmun2yong.global.kafka.outbox.relay

import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.global.lock.RedissonLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OutboxRelayScheduler(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(OutboxRelayScheduler::class.java)
    }

    @Scheduled(fixedDelay = 5000)
    @RedissonLock(key = "outboxRelay", leaseTime = 30, waitTime = 0)
    @Transactional
    fun relayPendingEvents() {
        outboxEventRepository
            .findByStatusAndCreatedAtBefore(
                OutboxStatus.PENDING,
                Instant.now().minusSeconds(30),
            ).forEach { event ->
                try {
                    kafkaEventPublisher.publish(
                        event.kafkaTopic,
                        event.kafkaKey,
                        event.kafkaPayload,
                    )
                    event.markPublished()
                } catch (e: Exception) {
                    log.error("Outbox relay 발행 실패: eventId={}", event.id, e)
                }
            }
    }
}
