package com.sbl.sulmun2yong.global.kafka.outbox.entity

enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
