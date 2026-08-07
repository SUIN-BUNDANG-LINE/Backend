package com.sbl.sulmun2yong.notification.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class KafkaSmsDltDispatcher(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val objectMapper: ObjectMapper,
) : DltDispatcher {
    companion object {
        const val DLT_TOPIC = "drawing-notification.DLT"
    }

    override fun dispatch(job: SmsNotificationJobEntity) {
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
            DLT_TOPIC,
            job.eventId,
            objectMapper.writeValueAsString(dltEvent),
        )
    }
}
