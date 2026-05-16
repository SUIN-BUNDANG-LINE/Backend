package com.sbl.sulmun2yong.drawing.listener

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException

class DrawingSmsNotificationEventListenerTest {
    private val smsNotificationJobRepository: SmsNotificationJobRepository = mock()
    private val applicationEventPublisher: ApplicationEventPublisher = mock()
    private val listener =
        DrawingSmsNotificationEventListener(smsNotificationJobRepository, applicationEventPublisher)

    @Test
    fun `당첨자가 아니면 잡 생성도 이벤트 발행도 하지 않는다`() {
        val event =
            DrawingCompletedNotificationConsumedEvent(
                eventId = "e1",
                surveyId = "s1",
                isWinner = false,
                rawPayload = "{}",
            )

        listener.handle(event)

        verify(smsNotificationJobRepository, never()).save(any())
        verify(applicationEventPublisher, never()).publishEvent(any())
    }

    @Test
    fun `당첨자면 SMS 잡 저장 후 SmsJobCreatedEvent를 발행한다`() {
        val event =
            DrawingCompletedNotificationConsumedEvent(
                eventId = "e1",
                surveyId = "s1",
                isWinner = true,
                rawPayload = "{}",
            )
        val saved = mock<SmsNotificationJobEntity> { on { id } doReturn 42L }
        whenever(smsNotificationJobRepository.save(any<SmsNotificationJobEntity>())).thenReturn(saved)

        listener.handle(event)

        verify(smsNotificationJobRepository).save(any<SmsNotificationJobEntity>())
        verify(applicationEventPublisher).publishEvent(SmsJobCreatedEvent(42L))
    }

    @Test
    fun `이미 등록된 잡(unique 충돌)이면 이벤트 발행 없이 정상 종료한다`() {
        val event =
            DrawingCompletedNotificationConsumedEvent(
                eventId = "e1",
                surveyId = "s1",
                isWinner = true,
                rawPayload = "{}",
            )
        whenever(smsNotificationJobRepository.save(any<SmsNotificationJobEntity>()))
            .thenThrow(DataIntegrityViolationException("dup"))

        listener.handle(event)

        verify(applicationEventPublisher, never()).publishEvent(any())
    }
}
