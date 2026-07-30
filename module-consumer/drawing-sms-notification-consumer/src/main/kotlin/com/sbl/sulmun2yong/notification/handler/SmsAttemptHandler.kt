package com.sbl.sulmun2yong.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.exception.SmsSendException
import com.sbl.sulmun2yong.notification.metrics.SmsNotificationMetrics
import com.sbl.sulmun2yong.notification.service.SmsNotificationJobService
import com.sbl.sulmun2yong.notification.service.SmsSender
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component

// SMS 발송 시도 1회의 전체 흐름을 책임진다.
// 발송 → 성공 시 잡 완료, 실패 시 markFailedOrRetry → 최종 실패면 DLT 발행 → metric 기록.
// SmsJobEventListener(happy path) 와 SmsNotificationJobWorker(recovery) 가 공유한다.
@Component
class SmsAttemptHandler(
    private val jobService: SmsNotificationJobService,
    private val smsSender: SmsSender,
    private val dltDispatcher: DltDispatcher,
    private val metrics: SmsNotificationMetrics,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SmsAttemptHandler::class.java)
        const val MAX_RETRY = 5
    }

    fun execute(job: SmsNotificationJobEntity) {
        // 사가 상관 키를 MDC 에 self-seed 한다. @Async 초기 시도·@Scheduled 재시도는 Kafka 인터셉터를
        // 타지 않으므로(스레드 전환) 여기서 직접 심어야, 모든 발송 시도 로그와 하류 발행(DLT)
        // 헤더가 사가 키(=originalDrawingEventId)를 실어 나른다. 발송 결과와 무관하게 finally 에서 정리.
        MDC.put("correlationId", job.eventId)
        try {
            val drawingCompletedEvent =
                objectMapper.readValue(job.payload, DrawingCompletedEvent::class.java)
            try {
                smsSender.sendWinnerNotification(drawingCompletedEvent)
                jobService.markCompleted(job.id)
                metrics.recordAttempt("success")
            } catch (e: SmsSendException) {
                val isFinalFailure =
                    jobService.markFailedOrRetry(job.id, e.message ?: "unknown", MAX_RETRY)
                if (isFinalFailure) {
                    dltDispatcher.dispatch(job)
                    log.error("SMS 최종 실패, DLT 발행: eventId={}", job.eventId, e)
                    metrics.recordAttempt("dlt")
                } else {
                    log.warn("SMS 발송 실패, 재시도 대기: eventId={}", job.eventId, e)
                    metrics.recordAttempt("retry")
                }
            }
        } finally {
            MDC.remove("correlationId")
        }
    }
}
