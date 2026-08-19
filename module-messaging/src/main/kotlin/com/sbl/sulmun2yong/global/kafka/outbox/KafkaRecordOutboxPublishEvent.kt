package com.sbl.sulmun2yong.global.kafka.outbox

import java.util.UUID

data class KafkaRecordOutboxPublishEvent(
    val outboxId: UUID,
    val kafkaTopic: String,
    val kafkaRecordKey: String,
    val kafkaRecordValue: String,
)
