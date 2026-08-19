package com.sbl.sulmun2yong.global.kafka.outbox.entity

enum class KafkaRecordOutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
