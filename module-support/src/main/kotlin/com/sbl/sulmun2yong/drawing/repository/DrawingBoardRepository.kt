package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingBoard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingBoardRepository : JpaRepository<DrawingBoard, UUID> {
    /** 보드 행만 읽는다 — 존재 확인처럼 티켓을 만지지 않는 곳에서 쓴다. */
    fun findBySurveyId(surveyId: UUID): Optional<DrawingBoard>

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

    // 아래 두 메서드는 낙관적 락 비교 실험(OptimisticRetryDrawingStrategy) 전용이다.
    // drawing_boards.version 은 JPA 매핑에서 제외돼 있어(@Version 없음) 이 경로로만 읽고 쓴다 —
    // 그래야 다른 네 전략의 추첨이 버전을 건드리지 않아 대조가 성립한다.

    @Query(value = "SELECT version FROM drawing_boards WHERE survey_id = :surveyId", nativeQuery = true)
    fun findVersionBySurveyId(
        @Param("surveyId") surveyId: UUID,
    ): Long?

    /** 읽어둔 버전이 그대로일 때만 1 올린다. 갱신 행 수가 0이면 그 사이 다른 추첨이 커밋된 것. */
    @Modifying(flushAutomatically = true)
    @Query(
        value = "UPDATE drawing_boards SET version = version + 1 WHERE survey_id = :surveyId AND version = :expectedVersion",
        nativeQuery = true,
    )
    fun increaseVersionIfUnchanged(
        @Param("surveyId") surveyId: UUID,
        @Param("expectedVersion") expectedVersion: Long,
    ): Int

    fun deleteBySurveyId(surveyId: UUID)
}
