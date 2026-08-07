package com.sbl.sulmun2yong.cofunding.repository

import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

// 사가 경합 구간의 상태 전이는 전부 조건부 UPDATE(CAS)다.
// 반환 1 = 이 트랜잭션이 전이의 승자, 0 = 조건 불충족 (패배 / 중복 / 이미 종료) - 호출자는 no-op.
interface CoFundingRepository : JpaRepository<CoFunding, UUID> {
    fun findBySurveyId(surveyId: UUID): CoFunding?

    // 장벽 판정 - SETTLED 행 수가 정원과 같을 때만 FUNDING -> CONFIRMED (모금 ④ 리스너가 호출).
    // 서브쿼리는 UPDATE 의 일부라 잠금 읽기(최신 커밋)로 평가된다 - MVCC 스냅샷이 아니다.
    // 반환 1 = 마지막 결제를 확인해 CONFIRMED 를 만든 유일한 승자 - 승자만 ⑤ 를 발행한다.
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
            WHERE p.fundingId = f.id AND p.status = 'SETTLED'
          )
      """,
    )
    fun tryConfirm(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
    ): Int

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

    // settle(D6)의 모금 상태 검사용 잠금 조회 - 무산 CAS(tryFail)와 행 잠금으로 직렬화해
    // "검사 통과 직후 무산 확정" 틈새(환불 누락 창)를 닫는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM CoFunding  f WHERE f.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: UUID,
    ): CoFunding?

    // 무산 판정 - 기한 만료 스케줄러가 두드리는 CAS. 승자만 failed 발행.
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

    // 판정 미회신 접수의 만료 종착 CAS - 기한 지난 PENDING_APPROVAL 을 일괄 REJECTED.
    // 승인/거절 리스너의 행 잠금과 상태 가드로 직렬화된다 - 반환값 = 종착시킨 행 수.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE CoFunding f
        SET f.status = 'REJECTED', f.updatedAt = :now
        WHERE f.status = 'PENDING_APPROVAL' AND f.deadline < :now
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
