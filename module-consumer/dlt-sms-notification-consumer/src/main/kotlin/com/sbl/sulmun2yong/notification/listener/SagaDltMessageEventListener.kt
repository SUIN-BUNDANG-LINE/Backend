package com.sbl.sulmun2yong.notification.listener

import com.sbl.sulmun2yong.notification.dto.event.SagaDltConsumedEvent
import com.sbl.sulmun2yong.notification.entity.DltMessageEntity
import com.sbl.sulmun2yong.notification.repository.DltMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Instant

// 사가 *.DLT 메시지를 dlt_messages 테이블로 영속화한다(조사·재처리용).
// SMS DLT(DltMessageEventListener)와 달리 하류 신호 발행은 없다 - 사가 실패는 알림 사가의
// '영구 실패' 신호(sms-delivery-permanently-failed)와 무관하므로 저장에서 종착한다.
@Component
class SagaDltMessageEventListener(
    private val dltMessageRepository: DltMessageRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SagaDltMessageEventListener::class.java)
    }

    @EventListener
    fun handle(event: SagaDltConsumedEvent) {
        dltMessageRepository.save(
            DltMessageEntity(
                eventId = event.eventId,
                // 어떤 사가 이벤트가 죽었는지는 원본 토픽명이 말해준다
                notificationType = event.originalTopic,
                payload = event.payload,
                // 리스너단 재시도는 발행측 에러핸들러가 소진 - 봉투가 없어 횟수는 전달되지 않는다
                retryCount = 0,
                lastError = event.exceptionMessage,
                failedAt = Instant.now(),
            ),
        )
        log.info("사가 DLT 메시지 저장 완료, topic:{}, eventId:{}", event.originalTopic, event.eventId)
    }
}
