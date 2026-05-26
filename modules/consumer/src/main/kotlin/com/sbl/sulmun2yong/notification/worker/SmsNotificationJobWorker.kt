package com.sbl.sulmun2yong.notification.worker

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.metrics.SmsNotificationMetrics
import com.sbl.sulmun2yong.notification.service.SmsNotificationJobService
import com.sbl.sulmun2yong.notification.service.SmsSender
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SmsNotificationJobWorker(
    private val smsNotificationJobService: SmsNotificationJobService,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val smsSender: SmsSender,
    private val objectMapper: ObjectMapper,
    private val metrics: SmsNotificationMetrics,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SmsNotificationJobWorker::class.java)
        private const val MAX_RETRY = 5
        private const val STALE_SECONDS = 30L
        private const val BATCH_SIZE = 10
    }

    @Scheduled(fixedDelay = 30000)
    fun processJobs() {
        val staleJobs = smsNotificationJobService.claimStaleJobs(STALE_SECONDS, BATCH_SIZE)
        staleJobs.forEach { job -> processSingleJob(job) }
    }

    private fun processSingleJob(job: SmsNotificationJobEntity) {
        try {
            val event = objectMapper.readValue(job.payload, DrawingCompletedEvent::class.java)
            smsSender.sendWinnerNotification(event)
            smsNotificationJobService.markCompleted(job.id)
            metrics.recordAttempt("success")
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
                metrics.recordAttempt("dlt")
            } else {
                log.warn("SMS 발송 실패, 재시도 대기: eventId={}", job.eventId, e)
                metrics.recordAttempt("retry")
            }
        }
    }
}
