package com.sbl.sulmun2yong.global.kafka.outbox.service

import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxEventEntity
import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class OutboxService(
    private val outboxEventRepository: OutboxEventRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(OutboxService::class.java)
        private const val RELAY_STALE_SECONDS = 60L
    }

    @Transactional
    fun claimPendingForRelay(limit: Int): List<OutboxEventEntity> =
        outboxEventRepository.findPendingForUpdateSkipLocked(
            OutboxStatus.PENDING,
            Instant.now().minusSeconds(RELAY_STALE_SECONDS),
            PageRequest.of(0, limit),
        )

    @Async("outboxAsyncExecutor")
    @Transactional
    fun markPublishedAsync(outboxId: UUID) {
        val outbox = outboxEventRepository.findById(outboxId).orElseThrow()
        outbox.markPublished()
    }

    @Async("outboxAsyncExecutor")
    @Transactional
    fun markFailedAsync(outboxId: UUID) {
        val outbox = outboxEventRepository.findById(outboxId).orElseThrow()
        outbox.markFailed()
    }

    @Async("outboxAsyncExecutor")
    @Transactional
    fun incrementRetryAsync(outboxId: UUID) {
        val outbox = outboxEventRepository.findById(outboxId).orElseThrow()
        if (outbox.incrementRetry()) {
            log.warn("FAILED 상태 전환됨, outboxId: {}", outboxId)
        }
    }
}
