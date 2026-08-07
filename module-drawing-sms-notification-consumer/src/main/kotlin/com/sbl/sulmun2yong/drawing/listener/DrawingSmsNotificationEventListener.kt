package com.sbl.sulmun2yong.drawing.listener

import com.sbl.sulmun2yong.drawing.dto.event.DrawingCompletedNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.NotificationTypes
import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

// 추첨 완료 이벤트 중 당첨자에 한해 SMS 발송 잡을 생성하고 후속 SmsJobCreatedEvent를 발행한다.
@Component
class DrawingSmsNotificationEventListener(
    private val smsNotificationJobRepository: SmsNotificationJobRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(DrawingSmsNotificationEventListener::class.java)
        private const val NOTIFICATION_TYPE = NotificationTypes.DRAWING_SMS
    }

    @EventListener
    fun handle(event: DrawingCompletedNotificationConsumedEvent) {
        if (!event.isWinner) return

        try {
            val job =
                smsNotificationJobRepository.save(
                    SmsNotificationJobEntity.create(
                        event.eventId,
                        NOTIFICATION_TYPE,
                        event.rawPayload,
                    ),
                )
            applicationEventPublisher.publishEvent(SmsJobCreatedEvent(job.id))
        } catch (e: DataIntegrityViolationException) {
            log.info("이미 등록된 작업입니다, eventId:{}, eventType:{}", event.eventId, NOTIFICATION_TYPE)
        }
    }
}
