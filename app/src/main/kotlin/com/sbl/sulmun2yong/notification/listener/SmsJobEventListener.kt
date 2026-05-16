package com.sbl.sulmun2yong.notification.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import com.sbl.sulmun2yong.notification.service.SmsNotificationJobService
import com.sbl.sulmun2yong.notification.service.SmsSender
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant

@Component
class SmsJobEventListener(
    private val smsNotificationJobRepository: SmsNotificationJobRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val smsNotificationJobService: SmsNotificationJobService,
    private val smsSender: SmsSender,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SmsJobEventListener::class.java)
        private const val MAX_RETRY = 5
    }

    @Async("smsJobExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onJobCreated(event: SmsJobCreatedEvent) {
        val job =
            smsNotificationJobRepository
                .findById(event.jobId)
                .orElseThrow { RuntimeException("존재하지 않는 job id입니다: " + event.jobId) }
        val drawingCompletedEvent =
            objectMapper.readValue(job.payload, DrawingCompletedEvent::class.java)

        try {
            smsSender.sendWinnerNotification(drawingCompletedEvent)
            smsNotificationJobService.markCompleted(job.id)
        } catch (e: Exception) {
            val isFinalFailure =
                smsNotificationJobService.markFailedOrRetry(
                    job.id,
                    e.message ?: "unknown",
                    MAX_RETRY,
                )
            if (isFinalFailure) {
                val dltEvent =
                    DltSmsNotificationEvent(
                        eventId = job.eventId,
                        payload = job.payload,
                        notificationType = job.notificationType,
                        retryCount = job.retryCount,
                        lastError = job.lastError,
                        failedAt = Instant.now(),
                    )
                kafkaEventPublisher.publish(
                    "drawing-notification.DLT",
                    job.eventId,
                    objectMapper.writeValueAsString(dltEvent),
                )
                log.error("SMS 최종 실패, DLT 발행: eventId={}", job.eventId, e)
            } else {
                log.warn("sms 전송 실패해서 재시도합니다", e)
            }
        }
    }
}
