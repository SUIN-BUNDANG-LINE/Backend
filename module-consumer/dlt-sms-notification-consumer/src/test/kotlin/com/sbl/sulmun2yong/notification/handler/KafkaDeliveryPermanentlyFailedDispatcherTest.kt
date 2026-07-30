package com.sbl.sulmun2yong.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.dto.event.SmsDeliveryPermanentlyFailedEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Instant

// KafkaDeliveryPermanentlyFailedDispatcher 의 외부 계약(토픽명·메시지 키·payload 직렬화)을 고정한다.
// 핵심: DLT 봉투의 payload(원본 drawing-completed JSON)에서 상관 키/식별자를 파싱해 신호를 조립한다.
class KafkaDeliveryPermanentlyFailedDispatcherTest {
    private lateinit var kafkaEventPublisher: KafkaEventPublisher
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
    private lateinit var dispatcher: KafkaDeliveryPermanentlyFailedDispatcher

    @BeforeEach
    fun setup() {
        kafkaEventPublisher = mock()
        dispatcher = KafkaDeliveryPermanentlyFailedDispatcher(kafkaEventPublisher, objectMapper)
    }

    private fun dltEvent(lastError: String?): DltSmsNotificationEvent {
        val original =
            DrawingCompletedPayload(
                eventId = "drawing-1",
                surveyId = "survey-1",
                isWinner = true,
                remainingTickets = 0,
                participantId = "participant-1",
            )
        return DltSmsNotificationEvent(
            eventId = "dlt-envelope-1",
            payload = objectMapper.writeValueAsString(original),
            notificationType = "DRAWING_SMS",
            retryCount = 6,
            lastError = lastError,
            failedAt = Instant.parse("2026-06-21T00:00:00Z"),
        )
    }

    @Test
    fun `dispatch 시 sms-delivery-permanently-failed 토픽에 originalDrawingEventId 키로 발행`() {
        dispatcher.dispatch(dltEvent(lastError = "gateway_down"))

        val payloadCaptor = argumentCaptor<String>()
        verify(kafkaEventPublisher).publish(
            eq("sms-delivery-permanently-failed"),
            eq("drawing-1"),
            payloadCaptor.capture(),
        )
        val published = objectMapper.readValue(payloadCaptor.firstValue, SmsDeliveryPermanentlyFailedEvent::class.java)
        assertEquals("drawing-1", published.originalDrawingEventId)
        assertEquals("participant-1", published.participantId)
        assertEquals("survey-1", published.surveyId)
        assertEquals("gateway_down", published.errorCode)
        assertEquals(Instant.parse("2026-06-21T00:00:00Z"), published.failedAt)
    }

    @Test
    fun `lastError 가 null 이면 errorCode 는 DLT_UNKNOWN 로 대체된다`() {
        dispatcher.dispatch(dltEvent(lastError = null))

        val payloadCaptor = argumentCaptor<String>()
        verify(kafkaEventPublisher).publish(eq("sms-delivery-permanently-failed"), eq("drawing-1"), payloadCaptor.capture())
        val published = objectMapper.readValue(payloadCaptor.firstValue, SmsDeliveryPermanentlyFailedEvent::class.java)
        assertEquals("DLT_UNKNOWN", published.errorCode)
    }
}
