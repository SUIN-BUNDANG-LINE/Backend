package com.sbl.sulmun2yong.global.kafka.outbox

import java.util.UUID

data class OutboxPublishEvent(
    val outboxId: UUID,
    val topic: String,
    val key: String,
    val payload: String,
)
