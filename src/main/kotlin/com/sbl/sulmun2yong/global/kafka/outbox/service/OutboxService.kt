package com.sbl.sulmun2yong.global.kafka.outbox.service

import com.sbl.sulmun2yong.global.kafka.outbox.repository.OutboxEventRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OutboxService(
    private val outboxEventRepository: OutboxEventRepository,
) {
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
}
