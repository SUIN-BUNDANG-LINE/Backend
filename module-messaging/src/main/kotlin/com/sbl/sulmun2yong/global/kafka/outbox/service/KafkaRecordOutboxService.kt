package com.sbl.sulmun2yong.global.kafka.outbox.service

import com.sbl.sulmun2yong.global.kafka.outbox.entity.KafkaRecordOutboxEntity
import com.sbl.sulmun2yong.global.kafka.outbox.entity.KafkaRecordOutboxStatus
import com.sbl.sulmun2yong.global.kafka.outbox.repository.KafkaRecordOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class KafkaRecordOutboxService(
    private val kafkaRecordOutboxRepository: KafkaRecordOutboxRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(KafkaRecordOutboxService::class.java)
    }

    // 정책값(배치 크기·신선도 유예)은 호출자인 릴레이가 결정해 넘긴다
    @Transactional
    fun claimPendingForRelay(
        limit: Int,
        staleSeconds: Long,
    ): List<KafkaRecordOutboxEntity> =
        kafkaRecordOutboxRepository.findPendingForUpdateSkipLocked(
            KafkaRecordOutboxStatus.PENDING,
            Instant.now().minusSeconds(staleSeconds),
            PageRequest.of(0, limit),
        )

    @Async("outboxAsyncExecutor")
    @Transactional
    fun markPublishedAsync(outboxId: UUID) {
        val outbox = kafkaRecordOutboxRepository.findById(outboxId).orElseThrow()
        outbox.markPublished()
    }

    @Async("outboxAsyncExecutor")
    @Transactional
    fun incrementRetryAsync(outboxId: UUID) {
        val outbox = kafkaRecordOutboxRepository.findById(outboxId).orElseThrow()
        if (outbox.incrementRetry()) {
            log.warn("FAILED 상태 전환됨, outboxId: {}", outboxId)
        }
    }
}
