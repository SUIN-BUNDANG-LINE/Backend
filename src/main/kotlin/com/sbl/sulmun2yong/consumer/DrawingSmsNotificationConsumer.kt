package com.sbl.sulmun2yong.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.consumer.event.KafkaAckEvent
import com.sbl.sulmun2yong.consumer.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.consumer.payload.DrawingCompletedPayload
import com.sbl.sulmun2yong.consumer.repository.SmsJobConsumerRepository
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DrawingSmsNotificationConsumer(
    private val objectMapper: ObjectMapper,
    private val smsJobConsumerRepository: SmsJobConsumerRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(DrawingSmsNotificationConsumer::class.java)
        private const val NOTIFICATION_TYPE = "DRAWING_SMS"
    }

    @KafkaListener(
        topics = ["drawing-completed"],
        groupId = "drawing-notification",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, DrawingCompletedPayload::class.java)

        if (!event.isWinner) {
            applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
            return
        }

        try {
            val job =
                smsJobConsumerRepository.save(
                    SmsNotificationJobEntity.create(
                        event.eventId,
                        NOTIFICATION_TYPE,
                        payload,
                    ),
                )
            applicationEventPublisher.publishEvent(SmsJobCreatedEvent(job.id))
        } catch (e: DataIntegrityViolationException) {
            log.info("이미 등록된 작업입니다, eventId:{}, eventType:{}", event.eventId, NOTIFICATION_TYPE)
            applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
            return
        }

        applicationEventPublisher.publishEvent(KafkaAckEvent(ack))
    }
}
