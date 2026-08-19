@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.sbl.sulmun2yong.payment.repository

import com.sbl.sulmun2yong.payment.entity.TossApiCallOutboxEntity
import com.sbl.sulmun2yong.payment.entity.TossApiCallStatus
import com.sbl.sulmun2yong.payment.entity.TossApiCallType
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface TossApiCallOutboxRepository : JpaRepository<TossApiCallOutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
            SELECT c FROM TossApiCallOutboxEntity c
            WHERE c.status = :status AND c.createdAt < :before
            ORDER BY c.createdAt ASC
        """,
    )
    fun findPendingForUpdateSkipLocked(
        @Param("status") status: TossApiCallStatus,
        @Param("before") before: Instant,
        pageable: Pageable,
    ): List<TossApiCallOutboxEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            UPDATE TossApiCallOutboxEntity c SET  c.status = 'SUPERSEDED'
            WHERE c.tossOrderId = :orderId AND c.callType = 'CONFIRM' AND c.status = 'PENDING'
        """,
    )
    fun supersedePendingConfirm(
        @Param("orderId") orderId: String,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            UPDATE TossApiCallOutboxEntity c
            SET c.status = SUCCEEDED, c.succeededAt = :now
            WHERE c.id = :id AND c.status = 'PENDING'
        """,
    )
    fun trySucceed(
        @Param("id") id: UUID,
        @Param("now") now: Instant,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            UPDATE TossApiCallOutboxEntity c
            SET c.status = FAILED
            WHERE c.id = :id AND c.status = 'PENDING'
        """,
    )
    fun tryFail(
        @Param("id") id: UUID,
    ): Int

    fun findByTossOrderIdAndCallTypeAndPaymentKey(
        tossOrderId: String,
        callType: TossApiCallType,
        paymentKey: String,
    ): TossApiCallOutboxEntity?

    fun existsByTossOrderIdAndCallType(
        tossOrderId: String,
        callType: TossApiCallType,
    ): Boolean

    fun existsByTossOrderIdAndCallTypeAndPaymentKey(
        tossOrderId: String,
        callType: TossApiCallType,
        paymentKey: String,
    ): Boolean
}
