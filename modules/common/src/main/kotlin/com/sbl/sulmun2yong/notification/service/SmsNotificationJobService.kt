package com.sbl.sulmun2yong.notification.service

import com.sbl.sulmun2yong.notification.entity.SmsJobStatus
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SmsNotificationJobService(
    private val smsNotificationJobRepository: SmsNotificationJobRepository,
) {
    @Transactional
    fun claimStaleJobs(
        staleSeconds: Long,
        limit: Int,
    ): List<SmsNotificationJobEntity> {
        val jobs =
            smsNotificationJobRepository.findStaleForUpdateSkipLocked(
                SmsJobStatus.PENDING,
                Instant.now().minusSeconds(staleSeconds),
                PageRequest.of(0, limit),
            )
        jobs.forEach { it.markExecuting() }
        return jobs
    }

    @Transactional
    fun markCompleted(id: Long) {
        val entity =
            smsNotificationJobRepository
                .findById(id)
                .orElseThrow { RuntimeException("Job not found: $id") }
        entity.markCompleted()
    }

    @Transactional
    fun markFailedOrRetry(
        id: Long,
        error: String,
        maxRetry: Int,
    ): Boolean {
        val entity =
            smsNotificationJobRepository
                .findById(id)
                .orElseThrow { RuntimeException("Job not found: $id") }
        return entity.markFailedOrRetry(error, maxRetry)
    }
}
