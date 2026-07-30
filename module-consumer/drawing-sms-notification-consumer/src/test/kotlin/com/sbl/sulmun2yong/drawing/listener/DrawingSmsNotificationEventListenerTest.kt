package com.sbl.sulmun2yong.drawing.listener

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException

// DrawingSmsNotificationEventListener 의 현재 동작을 고정한다.
// 멱등성(중복 eventId 저장 시 DataIntegrityViolationException 무시)이 깨지면 SMS 중복 발송 위험이 있다.
class DrawingSmsNotificationEventListenerTest {
    private lateinit var jobRepository: SmsNotificationJobRepository
    private lateinit var applicationEventPublisher: ApplicationEventPublisher
    private lateinit var listener: DrawingSmsNotificationEventListener

    @BeforeEach
    fun setup() {
        jobRepository = mock()
        applicationEventPublisher = mock()
        listener = DrawingSmsNotificationEventListener(jobRepository, applicationEventPublisher)
    }

    private fun event(isWinner: Boolean) =
        DrawingCompletedNotificationConsumedEvent(
            eventId = "evt-1",
            surveyId = "sv-1",
            isWinner = isWinner,
            rawPayload = """{"eventId":"evt-1"}""",
        )

    @Test
    fun `당첨자가 아니면 job 미저장 미발행`() {
        listener.handle(event(isWinner = false))

        verify(jobRepository, never()).save(any())
        verify(applicationEventPublisher, never()).publishEvent(any())
    }

    @Test
    fun `당첨자면 eventId notificationType payload 로 job 저장 후 SmsJobCreatedEvent 발행`() {
        val saved =
            SmsNotificationJobEntity(
                id = 99L,
                eventId = "evt-1",
                notificationType = "DRAWING_SMS",
                payload = """{"eventId":"evt-1"}""",
            )
        whenever(jobRepository.save(any<SmsNotificationJobEntity>())).thenReturn(saved)

        listener.handle(event(isWinner = true))

        val entityCaptor = argumentCaptor<SmsNotificationJobEntity>()
        verify(jobRepository).save(entityCaptor.capture())
        assertEquals("evt-1", entityCaptor.firstValue.eventId)
        assertEquals("DRAWING_SMS", entityCaptor.firstValue.notificationType)
        assertEquals("""{"eventId":"evt-1"}""", entityCaptor.firstValue.payload)

        verify(applicationEventPublisher).publishEvent(SmsJobCreatedEvent(99L))
    }

    @Test
    fun `중복 저장 시 DataIntegrityViolationException 은 무시되고 후속 이벤트 미발행`() {
        whenever(jobRepository.save(any<SmsNotificationJobEntity>())).thenThrow(DataIntegrityViolationException("dup"))

        listener.handle(event(isWinner = true))

        verify(applicationEventPublisher, never()).publishEvent(any())
    }
}
