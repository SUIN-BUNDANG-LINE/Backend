package com.sbl.sulmun2yong.global.kafka.outbox.repository

import com.sbl.sulmun2yong.global.kafka.outbox.entity.KafkaRecordOutboxEntity
import com.sbl.sulmun2yong.global.kafka.outbox.entity.KafkaRecordOutboxStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface KafkaRecordOutboxRepository : JpaRepository<KafkaRecordOutboxEntity, UUID> {
    fun countByStatus(status: KafkaRecordOutboxStatus): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // -2 = Hibernate LockOptions.SKIP_LOCKED
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        SELECT o FROM KafkaRecordOutboxEntity o
        WHERE o.status = :status AND o.createdAt < :before
        ORDER BY o.createdAt ASC
        """,
    )
    fun findPendingForUpdateSkipLocked(
        @Param("status") status: KafkaRecordOutboxStatus,
        @Param("before") before: Instant,
        pageable: Pageable,
    ): List<KafkaRecordOutboxEntity>
}
