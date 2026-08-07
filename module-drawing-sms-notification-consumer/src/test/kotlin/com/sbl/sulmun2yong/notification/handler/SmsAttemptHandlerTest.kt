package com.sbl.sulmun2yong.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.exception.SmsSendException
import com.sbl.sulmun2yong.notification.metrics.SmsNotificationMetrics
import com.sbl.sulmun2yong.notification.service.SmsNotificationJobService
import com.sbl.sulmun2yong.notification.service.SmsSender
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

// SmsAttemptHandler 의 동작을 고정한다.
// (기존 SmsJobEventListenerTest / SmsNotificationJobWorkerTest 의 동작 검증 케이스를 흡수)
class SmsAttemptHandlerTest {
    private lateinit var jobService: SmsNotificationJobService
    private lateinit var smsSender: SmsSender
    private lateinit var dltDispatcher: DltDispatcher
    private lateinit var metrics: SmsNotificationMetrics
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

    private lateinit var handler: SmsAttemptHandler

    private val drawingEvent =
        DrawingCompletedEvent(
            eventId = "evt-1",
            surveyId = "sv-1",
            participantId = "p-1",
            selectedNumber = 7,
            isWinner = true,
            rewardName = "Coffee",
            rewardCategory = "DRINK",
            remainingTickets = 0,
            timestamp = Instant.parse("2026-06-21T00:00:00Z"),
        )

    @BeforeEach
    fun setup() {
        jobService = mock()
        smsSender = mock()
        dltDispatcher = mock()
        metrics = mock()
        handler = SmsAttemptHandler(jobService, smsSender, dltDispatcher, metrics, objectMapper)
    }

    private fun job(
        retryCount: Int = 0,
        lastError: String? = null,
    ): SmsNotificationJobEntity {
        val entity =
            SmsNotificationJobEntity(
                id = 42L,
                eventId = "evt-1",
                notificationType = "DRAWING_SMS",
                payload = objectMapper.writeValueAsString(drawingEvent),
            )
        entity.retryCount = retryCount
        entity.lastError = lastError
        return entity
    }

    @Test
    fun `발송 성공 시 잡 완료(markCompleted) + metric success, 실패 처리 미호출`() {
        handler.execute(job())

        verify(smsSender).sendWinnerNotification(any())
        verify(jobService).markCompleted(42L)
        verify(metrics).recordAttempt("success")
        verify(jobService, never()).markFailedOrRetry(any(), any(), any())
        verify(dltDispatcher, never()).dispatch(any())
    }

    @Test
    fun `SmsSendException 일시 실패 시 markFailedOrRetry + metric retry, DLT 미발행`() {
        whenever(smsSender.sendWinnerNotification(any())).thenThrow(SmsSendException("network"))
        whenever(jobService.markFailedOrRetry(42L, "network", 5)).thenReturn(false)

        handler.execute(job(retryCount = 1))

        verify(jobService).markFailedOrRetry(42L, "network", 5)
        verify(metrics).recordAttempt("retry")
        verify(dltDispatcher, never()).dispatch(any())
        verify(jobService, never()).markCompleted(any())
    }

    @Test
    fun `SmsSendException 최종 실패 시 DltDispatcher dispatch + metric dlt`() {
        val finalJob = job(retryCount = 6, lastError = "fatal")
        whenever(smsSender.sendWinnerNotification(any())).thenThrow(SmsSendException("fatal"))
        whenever(jobService.markFailedOrRetry(42L, "fatal", 5)).thenReturn(true)

        handler.execute(finalJob)

        verify(jobService).markFailedOrRetry(42L, "fatal", 5)
        verify(dltDispatcher).dispatch(finalJob)
        verify(metrics).recordAttempt("dlt")
    }

    @Test
    fun `SmsSendException 외 일반 RuntimeException 은 catch 되지 않고 전파, 재시도 회로 진입 안 함`() {
        whenever(smsSender.sendWinnerNotification(any())).thenThrow(IllegalStateException("system"))

        assertThrows(IllegalStateException::class.java) { handler.execute(job()) }

        verify(jobService, never()).markFailedOrRetry(any(), any(), any())
        verify(jobService, never()).markCompleted(any())
        verify(dltDispatcher, never()).dispatch(any())
        verify(metrics, never()).recordAttempt(any())
    }

    @Test
    fun `MAX_RETRY 는 5 로 고정`() {
        whenever(smsSender.sendWinnerNotification(any())).thenThrow(SmsSendException("x"))
        whenever(jobService.markFailedOrRetry(any(), any(), eq(5))).thenReturn(false)

        handler.execute(job())

        verify(jobService).markFailedOrRetry(any(), any(), eq(5))
    }
}
