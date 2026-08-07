package com.sbl.sulmun2yong.cofunding.listener

import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.NotificationTypes
import com.sbl.sulmun2yong.notification.dto.event.SmsJobCreatedEvent
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
import com.sbl.sulmun2yong.notification.repository.SmsNotificationJobRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

// 모금 무산 이벤트에 대해 참여자 안내 SMS 발송 잡을 생성하고 후속 SmsJobCreatedEvent를 발행한다.
@Component
class CoFundingSmsNotificationEventListener(
    private val smsNotificationJobRepository: SmsNotificationJobRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingSmsNotificationEventListener::class.java)
    }

    @EventListener
    fun handle(event: CoFundingFailedNotificationConsumedEvent) {
        try {
            val job =
                smsNotificationJobRepository.save(
                    SmsNotificationJobEntity.create(
                        event.eventId,
                        NotificationTypes.CO_FUNDING_FAILED_SMS,
                        event.rawPayload,
                    ),
                )
            applicationEventPublisher.publishEvent(SmsJobCreatedEvent(job.id))
        } catch (e: DataIntegrityViolationException) {
            log.info("이미 등록된 작업입니다, eventId:{}, eventType:{}", event.eventId, NotificationTypes.CO_FUNDING_FAILED_SMS)
        }
    }
}
