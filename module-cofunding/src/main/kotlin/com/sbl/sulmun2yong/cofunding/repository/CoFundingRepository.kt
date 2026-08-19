package com.sbl.sulmun2yong.cofunding.repository

import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface CoFundingRepository : JpaRepository<CoFunding, UUID> {
    fun existsBySurveyIdAndStatusIn(
        surveyId: UUID,
        statuses: Collection<CoFundingStatus>,
    ): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE CoFunding f
        SET f.status = CONFIRMED,
            f.updatedAt = :now
        WHERE f.id = :id
          AND f.status = 'FUNDING'
          AND f.capacity = (
            SELECT COUNT(p) FROM CoFundingParticipant p
            WHERE p.fundingId = f.id AND p.status = 'PAID'
          )
      """,
    )
    fun tryConfirm(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
    ): Int

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
              AND p.status = PAID)""",
    )
    fun tryMarkRefunded(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM CoFunding  f WHERE f.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: UUID,
    ): CoFunding?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE CoFunding f
        SET f.status = FAILED, f.updatedAt = :now
        WHERE f.id = :id AND f.status = 'FUNDING'
    """,
    )
    fun tryFail(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
    ): Int

    // 판정 미회신 접수의 만료 종착 CAS - 기한 지난 PENDING 을 일괄 REJECTED.
    // 승인/거절 리스너의 행 잠금과 상태 가드로 직렬화된다 - 반환값 = 종착시킨 행 수.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE CoFunding f
        SET f.status = 'REJECTED', f.updatedAt = :now
        WHERE f.status = 'PENDING' AND f.deadline < :now
    """,
    )
    fun rejectExpiredPendingApprovals(
        @Param("now") now: LocalDateTime,
    ): Int

    // 기한 스케줄러 클레임 - 만료된 FUNDING 을 잠그고 집는다.
    // SKIP LOCKED(-2): 다른 인스턴스가 잠근 행은 건너뛴다 - 스케줄러 다중 인스턴스 분업.
    // ④ 리스너의 findByIdForUpdate 와 같은 행 잠금이라 "만료 처리 vs 마지막 결제"도 직렬화된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
            SELECT f FROM  CoFunding  f
            WHERE f.status  = 'FUNDING' AND f.deadline < :now
            ORDER BY f.deadline ASC
        """,
    )
    fun findExpiredForUpdateSkipLocked(
        @Param("now") now: LocalDateTime,
        pageable: PageRequest,
    ): List<CoFunding>
}
