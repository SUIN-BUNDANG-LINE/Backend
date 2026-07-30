package com.sbl.sulmun2yong.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

// KafkaSmsDltDispatcher 의 외부 계약(토픽명·메시지 키·payload 직렬화)을 고정한다.
class KafkaSmsDltDispatcherTest {
    private lateinit var kafkaEventPublisher: KafkaEventPublisher
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
    private lateinit var dispatcher: KafkaSmsDltDispatcher

    @BeforeEach
    fun setup() {
        kafkaEventPublisher = mock()
        dispatcher = KafkaSmsDltDispatcher(kafkaEventPublisher, objectMapper)
    }

    @Test
    fun `dispatch 시 drawing-notification_DLT 토픽에 DltSmsNotificationEvent 직렬화 발행`() {
        val job =
            SmsNotificationJobEntity(
                id = 1L,
                eventId = "evt-1",
                notificationType = "DRAWING_SMS",
                payload = """{"foo":"bar"}""",
            )
        job.retryCount = 6
        job.lastError = "fatal"

        dispatcher.dispatch(job)

        val payloadCaptor = argumentCaptor<String>()
        verify(kafkaEventPublisher).publish(
            eq("drawing-notification.DLT"),
            eq("evt-1"),
            payloadCaptor.capture(),
        )
        val published = objectMapper.readValue(payloadCaptor.firstValue, DltSmsNotificationEvent::class.java)
        assertEquals("evt-1", published.eventId)
        assertEquals("DRAWING_SMS", published.notificationType)
        assertEquals("""{"foo":"bar"}""", published.payload)
        assertEquals(6, published.retryCount)
        assertEquals("fatal", published.lastError)
    }
}
