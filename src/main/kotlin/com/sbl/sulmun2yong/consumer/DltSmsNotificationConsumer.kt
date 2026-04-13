package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationEvent
import com.sbl.sulmun2yong.notification.entity.DltMessageEntity
import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class DltSmsNotificationConsumer(
    private val dltMessageRepository: DltMessageRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log = LoggerFactory.getLogger(DltSmsNotificationConsumer::class.java)
    }

    @KafkaListener(topics = ["drawing-notification.DLT"], groupId = "dlt-sms-notification")
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        // TODO 1: payload → DltSmsNotificationEvent 역직렬화
        val event = objectMapper.readValue(payload, DltSmsNotificationEvent::class.java)
        // TODO 2: DltMessageEntity 생성 + save
        dltMessageRepository.save(
            DltMessageEntity(
                eventId = event.eventId,
                notificationType = event.notificationType,
                payload = event.payload,
                retryCount = event.retryCount,
                lastError = event.lastError,
                failedAt = event.failedAt,
            ),
        )
        // TODO 3: log.error("DLT 메시지 저장 완료")
        log.info("DLT 메시지 저장 완료, eventId:{}", event.eventId)

        // TODO 4: ack
        ack.acknowledge()
    }
}
