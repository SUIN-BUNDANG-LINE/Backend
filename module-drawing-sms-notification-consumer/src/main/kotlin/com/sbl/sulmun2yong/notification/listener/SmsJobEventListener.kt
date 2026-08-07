package com.sbl.sulmun2yong.notification.listener

import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.handler.SmsAttemptHandler
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SmsJobEventListener(
    private val smsNotificationJobRepository: SmsNotificationJobRepository,
    private val smsAttemptHandler: SmsAttemptHandler,
) {
    @Async("smsJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onJobCreated(event: SmsJobCreatedEvent) {
        val job =
            smsNotificationJobRepository
                .findById(event.jobId)
                .orElseThrow { RuntimeException("존재하지 않는 job id입니다: " + event.jobId) }
        smsAttemptHandler.execute(job)
    }
}
