package com.sbl.sulmun2yong.notification.repository

import com.sbl.sulmun2yong.notification.entity.SmsJobStatus
import com.sbl.sulmun2yong.notification.entity.SmsNotificationJobEntity
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

@Repository
interface SmsNotificationJobRepository : JpaRepository<SmsNotificationJobEntity, Long> {
    fun countByStatus(status: SmsJobStatus): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        SELECT j FROM SmsNotificationJobEntity j
        WHERE j.status = :status AND j.nextAttemptAt <= :threshold
        ORDER BY j.nextAttemptAt ASC
        """,
    )
    fun findStaleForUpdateSkipLocked(
        @Param("status") status: SmsJobStatus,
        @Param("threshold") threshold: Instant,
        pageable: Pageable,
    ): List<SmsNotificationJobEntity>
}
