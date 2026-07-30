package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.entity.Survey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface SurveyRepository : JpaRepository<Survey, UUID> {
    // 설문 활성화 CAS - 결제 대기였을 때만 연다. 장벽 CAS 승자(집계 리스너)가 호출.
    // 반환 1 = 활성화 수행, 0 = 이미 열렸거나 대기 상태 아님 - no-op (재수신 멱등)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE Survey s
        SET s.status = 'IN_PROGRESS', s.updatedAt = :now
        WHERE s.id = :id AND s.status = 'PENDING_PAYMENT'
    """,
    )
    fun tryActivate(
        @Param("id") id: UUID,
        @Param("now") now: LocalDateTime,
    ): Int
}
