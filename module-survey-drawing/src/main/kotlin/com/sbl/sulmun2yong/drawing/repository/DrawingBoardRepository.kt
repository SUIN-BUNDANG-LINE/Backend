package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import com.sbl.sulmun2yong.drawing.entity.DrawingBoardStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingBoardRepository : JpaRepository<DrawingBoard, UUID> {
    /** 보드 행만 읽는다 — 존재 확인처럼 티켓을 만지지 않는 곳에서 쓴다. */
    fun findBySurveyId(surveyId: UUID): Optional<DrawingBoard>

    /** 결제 대기(PENDING_PAYMENT) 보드 존재 = 설문 수정 잠금 — 편집 가드가 조회한다. */
    fun existsBySurveyIdAndStatus(
        surveyId: UUID,
        status: DrawingBoardStatus,
    ): Boolean

    /**
     * 티켓까지 한 문장으로 읽는다.
     *
     * 티켓을 쓸 곳에서 기본 조회를 쓰면 컬렉션을 만지는 순간 지연 로딩 조회가 따로 나간다.
     * 그 조회는 리포지토리 밖(엔티티 필드 접근)에서 일어나 Spring 예외 변환을 타지 않으므로,
     * 거기서 난 데드락이 Hibernate 고유 예외로 새어 나온다.
     */
    @Query("SELECT b FROM DrawingBoard b JOIN FETCH b.ticketEntities WHERE b.surveyId = :surveyId")
    fun findBySurveyIdWithTickets(
        @Param("surveyId") surveyId: UUID,
    ): Optional<DrawingBoard>

}
