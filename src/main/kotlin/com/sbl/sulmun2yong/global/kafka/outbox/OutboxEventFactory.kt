package com.sbl.sulmun2yong.global.kafka.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxEventEntity
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class OutboxEventFactory(
    private val objectMapper: ObjectMapper,
) {
    fun create(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        kafkaTopic: String,
        event: Any,
    ): OutboxEventEntity =
        OutboxEventEntity(
            id = UUID.randomUUID(),
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            eventType = eventType,
            kafkaTopic = kafkaTopic,
            kafkaKey = aggregateId,
            kafkaPayload = objectMapper.writeValueAsString(event),
            createdAt = Instant.now(),
        )
}
