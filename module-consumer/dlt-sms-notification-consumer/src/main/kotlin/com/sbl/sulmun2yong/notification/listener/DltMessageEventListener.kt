package com.sbl.sulmun2yong.notification.listener

import com.sbl.sulmun2yong.notification.dto.event.DltSmsNotificationConsumedEvent
import com.sbl.sulmun2yong.notification.entity.DltMessageEntity
import com.sbl.sulmun2yong.notification.handler.DeliveryPermanentlyFailedDispatcher
import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

// DLT 토픽으로 라우팅된 SMS 실패 메시지를 dlt_messages 테이블로 영속화하고,
// 커밋 후 사가 하류로 '영구 실패' 신호를 발행한다.
@Component
class DltMessageEventListener(
    private val dltMessageRepository: DltMessageRepository,
    private val deliveryPermanentlyFailedDispatcher: DeliveryPermanentlyFailedDispatcher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(DltMessageEventListener::class.java)
    }

    // ① 저장 — KafkaListener 트랜잭션 안에서 동기 실행된다.
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

    // ② 발행 — DB 커밋이 끝난 뒤에만 사가 하류로 영구 실패 신호를 쏜다(FR-009).
    //    롤백되면 이 메서드는 호출되지 않아 유령 신호가 나가지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCommitted(consumed: DltSmsNotificationConsumedEvent) {
        deliveryPermanentlyFailedDispatcher.dispatch(consumed.event)
        log.info("영구 실패 신호 발행 완료, eventId:{}", consumed.event.eventId)
    }
}
