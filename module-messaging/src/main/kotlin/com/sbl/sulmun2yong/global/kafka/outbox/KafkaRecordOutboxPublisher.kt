package com.sbl.sulmun2yong.global.kafka.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.DomainEvent
import com.sbl.sulmun2yong.global.kafka.outbox.entity.KafkaRecordOutboxEntity
import com.sbl.sulmun2yong.global.kafka.outbox.repository.KafkaRecordOutboxRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.*

@Component
class KafkaRecordOutboxPublisher(
    private val objectMapper: ObjectMapper,
    private val kafkaRecordOutboxRepository: KafkaRecordOutboxRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    // 행 PK 는 페이로드의 eventId 를 그대로 쓴다 - 컨슈머가 남긴 eventId 로
    // 프로듀서 쪽 발행 이력을 PK 조회 한 번에 되짚을 수 있다.
    fun publish(
        kafkaTopic: String,
        kafkaRecordKey: String,
        kafkaRecordValue: DomainEvent,
    ) {
        val outboxEvent =
            KafkaRecordOutboxEntity.create(
                eventId = UUID.fromString(kafkaRecordValue.eventId),
                kafkaTopic = kafkaTopic,
                kafkaRecordKey = kafkaRecordKey,
                kafkaRecordValue = objectMapper.writeValueAsString(kafkaRecordValue),
            )

        kafkaRecordOutboxRepository.save(outboxEvent)

        applicationEventPublisher.publishEvent(
            KafkaRecordOutboxPublishEvent(
                outboxId = outboxEvent.id,
                kafkaTopic = outboxEvent.kafkaTopic,
                kafkaRecordKey = outboxEvent.kafkaRecordKey,
                kafkaRecordValue = outboxEvent.kafkaRecordValue,
            ),
        )
    }
}
