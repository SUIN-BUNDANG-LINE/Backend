package com.sbl.sulmun2yong.cofunding.repository

import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

// 사가 경합 구간의 상태 전이는 전부 조건부 UPDATE(CAS)다.
// 반환 1 = 이 트랜잭션이 전이의 승자, 0 = 조건 불충족 (패배 / 중복 / 이미 종료) - 호출자는 no-op.
interface CoFundingRepository : JpaRepository<CoFunding, UUID> {
    fun findBySurveyId(surveyId: UUID): CoFunding?

    // 환불 수렴 CAS - 릴레이의 CANCEL 승인 확정 후처리가 호출.
    // SETTLED 잔여 참여자가 0명이 된 시점에만 FAILED -> REFUNDED.
    // 반환 1 = 이 트랜잭셔 전이의 승자, 0 = 아직 잔여 있음 / 이미 전이됨 - no-op.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            UPDATE CoFunding f
            SET f.status = REFUNDED,
                f.updatedAt = :now
            WHERE f.id = :id
            AND f.status = FAILED
            AND NOT EXISTS (
              SELECT p FROM CoFundingParticipant p
              WHERE p.fundingId = f.id
              AND p.status = SETTLED)""",
    )
    fun tryMarkRefunded(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
    ): Int
}
