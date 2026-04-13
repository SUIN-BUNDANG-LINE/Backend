package com.sbl.sulmun2yong.global.kafka.outbox.repository

import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxEventEntity
import com.sbl.sulmun2yong.global.kafka.outbox.entity.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface OutboxEventRepository : JpaRepository<OutboxEventEntity, UUID> {
    fun findByStatusAndCreatedAtBefore(
        status: OutboxStatus,
        before: Instant,
    ): List<OutboxEventEntity>
}
