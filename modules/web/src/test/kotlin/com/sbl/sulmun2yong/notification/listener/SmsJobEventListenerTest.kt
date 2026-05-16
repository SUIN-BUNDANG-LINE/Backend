package com.sbl.sulmun2yong.notification.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedEvent
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import com.sbl.sulmun2yong.notification.service.SmsNotificationJobService
import com.sbl.sulmun2yong.notification.service.SmsSender
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.Optional

class SmsJobEventListenerTest {
    private val smsNotificationJobRepository: SmsNotificationJobRepository = mock()
    private val kafkaEventPublisher: KafkaEventPublisher = mock()
    private val smsNotificationJobService: SmsNotificationJobService = mock()
    private val smsSender: SmsSender = mock()
    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val listener =
        SmsJobEventListener(
            smsNotificationJobRepository,
            kafkaEventPublisher,
            smsNotificationJobService,
            smsSender,
            objectMapper,
        )

    private fun mockJob(payload: String = sampleDrawingCompletedPayload()): SmsNotificationJobEntity =
        mock {
            on { id } doReturn 1L
            on { eventId } doReturn "evt-1"
            on { this.payload } doReturn payload
            on { notificationType } doReturn "DRAWING_SMS"
            on { retryCount } doReturn 0
            on { lastError } doReturn null
        }

    private fun sampleDrawingCompletedPayload(): String =
        objectMapper.writeValueAsString(
            DrawingCompletedEvent(
                eventId = "evt-1",
                surveyId = "s1",
                participantId = "p1",
                selectedNumber = 0,
                isWinner = true,
                rewardName = "스타벅스",
                rewardCategory = "커피",
                remainingTickets = 0,
                timestamp = Instant.now(),
            ),
        )

    @Test
    fun `SMS 발송 성공 시 markCompleted 호출, DLT 발행 안 함`() {
        val job = mockJob()
        whenever(smsNotificationJobRepository.findById(1L)).thenReturn(Optional.of(job))

        listener.onJobCreated(SmsJobCreatedEvent(1L))

        verify(smsSender).sendWinnerNotification(any())
        verify(smsNotificationJobService).markCompleted(1L)
        verify(kafkaEventPublisher, never()).publish(any(), any(), any())
    }

    @Test
    fun `재시도 가능한 실패면 markFailedOrRetry만 호출하고 DLT 발행 안 함`() {
        val job = mockJob()
        whenever(smsNotificationJobRepository.findById(1L)).thenReturn(Optional.of(job))
        whenever(smsSender.sendWinnerNotification(any())).thenThrow(RuntimeException("temporary"))
        whenever(smsNotificationJobService.markFailedOrRetry(eq(1L), any(), eq(5))).thenReturn(false)

        listener.onJobCreated(SmsJobCreatedEvent(1L))

        verify(smsNotificationJobService).markFailedOrRetry(eq(1L), any(), eq(5))
        verify(kafkaEventPublisher, never()).publish(any(), any(), any())
    }

    @Test
    fun `최대 재시도 초과(최종 실패)면 DLT 토픽에 발행한다`() {
        val job = mockJob()
        whenever(smsNotificationJobRepository.findById(1L)).thenReturn(Optional.of(job))
        whenever(smsSender.sendWinnerNotification(any())).thenThrow(RuntimeException("permanent"))
        whenever(smsNotificationJobService.markFailedOrRetry(eq(1L), any(), eq(5))).thenReturn(true)

        listener.onJobCreated(SmsJobCreatedEvent(1L))

        verify(kafkaEventPublisher).publish(eq("drawing-notification.DLT"), eq("evt-1"), any())
    }

    @Test
    fun `존재하지 않는 jobId면 예외를 던진다`() {
        whenever(smsNotificationJobRepository.findById(99L)).thenReturn(Optional.empty())

        try {
            listener.onJobCreated(SmsJobCreatedEvent(99L))
            org.junit.jupiter.api.Assertions.fail("예외가 발생해야 한다")
        } catch (_: RuntimeException) {
            // expected
        }
    }
}
