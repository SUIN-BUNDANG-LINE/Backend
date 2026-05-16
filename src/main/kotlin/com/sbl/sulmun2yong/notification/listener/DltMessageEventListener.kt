package com.sbl.sulmun2yong.notification.listener

import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.entity.DltMessageEntity
import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

// DLT 토픽으로 라우팅된 SMS 실패 메시지를 dlt_messages 테이블로 영속화한다.
@Component
class DltMessageEventListener(
    private val dltMessageRepository: DltMessageRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(DltMessageEventListener::class.java)
    }

    @EventListener
    fun handle(consumed: DltSmsNotificationConsumedEvent) {
        val event = consumed.event
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
        log.info("DLT 메시지 저장 완료, eventId:{}", event.eventId)
    }
}
