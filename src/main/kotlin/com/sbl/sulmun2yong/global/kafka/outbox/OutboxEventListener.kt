package com.sbl.sulmun2yong.global.kafka.outbox

import com.sbl.sulmun2yong.global.kafka.outbox.service.OutboxService
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OutboxEventListener(
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val outboxService: OutboxService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: OutboxPublishEvent) {
        kafkaEventPublisher
            .publish(
                topic = event.topic,
                key = event.key,
                payload = event.payload,
            ).whenComplete { _, ex ->
                if (ex == null) {
                    outboxService.markPublishedAsync(event.outboxId)
                }
                // 실패 시: PENDING 유지 → Relay가 재발행
                // FAILED 마킹은 Phase 2에서 DLQ 연동 시 최종 실패에만 사용
            }
    }
}
