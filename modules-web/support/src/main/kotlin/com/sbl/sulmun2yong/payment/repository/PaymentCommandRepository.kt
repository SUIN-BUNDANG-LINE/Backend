@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.sbl.sulmun2yong.payment.repository

import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface PaymentCommandRepository : JpaRepository<PaymentCommand, UUID> {

    // 릴레이 클레임: 미확정 커맨드(PENDING=미발송, SENT=응답 유실)를 잠그고 집는다.
    // SKIP LOCKED(-2): 다른 인스턴스가 잠근 행은 기다리지 않고 건너 뛴다 - web 2대의 분업.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
            SELECT c FROM PaymentCommand c
            WHERE c.status IN :statuses AND c.createdAt < :before
            ORDER BY c.createdAt ASC
        """,
    )
    fun findRetryableForUpdateSkipLocked(
        @Param("statuses") statuses: Collection<PaymentCommandStatus>,
        @Param("before") before: Instant,
        pageable: Pageable,
    ): List<PaymentCommand>
}
