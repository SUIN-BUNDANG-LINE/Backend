package com.sbl.sulmun2yong.notification.worker

import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.handler.SmsAttemptHandler
import com.sbl.sulmun2yong.notification.service.SmsNotificationJobService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

// 리팩터링 후 Worker 는 "claimStaleJobs → SmsAttemptHandler 순차 위임"만 책임진다.
// 발송/실패/DLT 동작은 SmsAttemptHandlerTest 에서 검증한다.
class SmsNotificationJobWorkerTest {
    private lateinit var jobService: SmsNotificationJobService
    private lateinit var smsAttemptHandler: SmsAttemptHandler
    private lateinit var worker: SmsNotificationJobWorker

    private val job1 =
        SmsNotificationJobEntity(id = 11L, eventId = "evt-1", notificationType = "DRAWING_SMS", payload = "{}")
    private val job2 =
        SmsNotificationJobEntity(id = 22L, eventId = "evt-2", notificationType = "DRAWING_SMS", payload = "{}")

    @BeforeEach
    fun setup() {
        jobService = mock()
        smsAttemptHandler = mock()
        worker = SmsNotificationJobWorker(jobService, smsAttemptHandler)
    }

    @Test
    fun `STALE_SECONDS=30 BATCH_SIZE=10 으로 claimStaleJobs 호출`() {
        whenever(jobService.claimStaleJobs(30L, 10)).thenReturn(emptyList())

        worker.processJobs()

        verify(jobService).claimStaleJobs(30L, 10)
        verify(smsAttemptHandler, never()).execute(any())
    }

    @Test
    fun `반환된 잡 각각을 SmsAttemptHandler 에 순차 위임`() {
        whenever(jobService.claimStaleJobs(any(), any())).thenReturn(listOf(job1, job2))

        worker.processJobs()

        verify(smsAttemptHandler).execute(job1)
        verify(smsAttemptHandler).execute(job2)
    }
}
