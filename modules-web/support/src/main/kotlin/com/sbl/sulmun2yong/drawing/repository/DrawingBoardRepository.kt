package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingBoardRepository : JpaRepository<DrawingBoard, UUID> {
    fun findBySurveyId(surveyId: UUID): Optional<DrawingBoard>

    // 낙관적 락 실험용 — 조회만 해도 커밋 시 보드 version 을 강제 증가시켜,
    // 같은 보드에 겹친 추첨 트랜잭션 중 나중 커밋이 OptimisticLockException 으로 실패한다.
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT b FROM DrawingBoard b WHERE b.surveyId = :surveyId")
    fun findBySurveyIdWithOptimisticForceIncrement(
        @Param("surveyId") surveyId: UUID,
    ): Optional<DrawingBoard>

    fun deleteBySurveyId(surveyId: UUID)
}
