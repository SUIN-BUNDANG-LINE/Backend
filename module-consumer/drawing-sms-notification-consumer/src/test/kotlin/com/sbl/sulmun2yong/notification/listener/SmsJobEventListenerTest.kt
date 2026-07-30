package com.sbl.sulmun2yong.notification.listener

import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.handler.SmsAttemptHandler
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

// 리팩터링 후 SmsJobEventListener 는 "잡 조회 → SmsAttemptHandler 위임"만 책임진다.
// 실제 발송/실패/DLT 동작은 SmsAttemptHandlerTest 에서 검증한다.
class SmsJobEventListenerTest {
    private lateinit var jobRepository: SmsNotificationJobRepository
    private lateinit var smsAttemptHandler: SmsAttemptHandler
    private lateinit var listener: SmsJobEventListener

    private val job =
        SmsNotificationJobEntity(
            id = 42L,
            eventId = "evt-1",
            notificationType = "DRAWING_SMS",
            payload = "{}",
        )

    @BeforeEach
    fun setup() {
        jobRepository = mock()
        smsAttemptHandler = mock()
        listener = SmsJobEventListener(jobRepository, smsAttemptHandler)
    }

    @Test
    fun `잡 조회 후 SmsAttemptHandler 에 위임한다`() {
        whenever(jobRepository.findById(42L)).thenReturn(Optional.of(job))

        listener.onJobCreated(SmsJobCreatedEvent(42L))

        verify(smsAttemptHandler).execute(job)
    }

    @Test
    fun `존재하지 않는 jobId 면 RuntimeException 던지고 handler 미호출`() {
        whenever(jobRepository.findById(42L)).thenReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            listener.onJobCreated(SmsJobCreatedEvent(42L))
        }
        verify(smsAttemptHandler, never()).execute(any())
    }
}
