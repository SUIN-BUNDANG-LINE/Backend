package com.sbl.sulmun2yong.cofunding.repository

import com.sbl.sulmun2yong.cofunding.entity.CoFunding
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

// 컨슈머가 호출하는 CAS 만 둔다 - 환불 수렴(tryMarkRefunded)은 web 릴레이 소관(D5).
// 반환 1 = 이 트랜잭션이 전이의 승자, 0 = 조건 불충족(패배/중복/이미 종료) - 호출자는 no-op.
interface CoFundingRepository : JpaRepository<CoFunding, UUID> {
    // 장벽 판정 - SETTLED 행 수가 정원과 같을 때만 FUNDING -> CONFIRMED.
    // 서브쿼리는 UPDATE 의 일부라 잠금 읽기(최신 커밋)로 평가된다 - MVCC 스냅샷이 아니다.
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

    // 기한 스케줄러 클레임 - 만료된 FUNDING 을 잠그고 집는다.
    // SKIP LOCKED(-2): 다른 인스턴스가 잠근 행은 건너뛴다. - 스케줄러 다중 인스턴스 분업.
    // settle 의 findByIdForUpdate 와 같은 행 잠금이라 "만료 처리 vs 마지막 결제"도 직렬화된다.
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

    // 환불 수렴 CAS 의 컨슈머 사본 - 결제자 0명 무산 전용 (D5 예외).
    // CANCEL 이 0건이면 릴레이 후처리가 발동할 수 없어, 환불 리스너가 직접 종착시킨다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE CoFunding  f
        SET f.status = REFUNDED, f.updatedAt = :now
        WHERE f.id = :id AND f.status = 'FAILED'
          AND NOT EXISTS (
            SELECT p FROM CoFundingParticipant  p
            WHERE p.fundingId = f.id AND p.status = 'SETTLED'
          )
    """,
    )
    fun tryMarkRefunded(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
    ): Int
}
