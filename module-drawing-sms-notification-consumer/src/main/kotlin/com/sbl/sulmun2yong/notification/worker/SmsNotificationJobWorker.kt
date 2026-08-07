package com.sbl.sulmun2yong.notification.worker

import com.sbl.sulmun2yong.notification.handler.SmsAttemptHandler
import com.sbl.sulmun2yong.notification.service.SmsNotificationJobService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SmsNotificationJobWorker(
    private val smsNotificationJobService: SmsNotificationJobService,
    private val smsAttemptHandler: SmsAttemptHandler,
) {
    companion object {
        private const val STALE_SECONDS = 30L
        private const val BATCH_SIZE = 10
    }

    @Scheduled(fixedDelay = 30000)
    fun processJobs() {
        smsNotificationJobService
            .claimStaleJobs(STALE_SECONDS, BATCH_SIZE)
            .forEach { smsAttemptHandler.execute(it) }
    }
}
