package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DrawingHistoryRepository : JpaRepository<DrawingHistory, UUID> {
    fun findBySurveyId(surveyId: UUID): List<DrawingHistory>

    @Query("SELECT h FROM DrawingHistory h WHERE h.surveyId = :surveyId AND h.ticketType = 'WINNING'")
    fun findBySurveyIdForWinner(
        @Param("surveyId") surveyId: UUID,
    ): List<DrawingHistory>

    @Query("SELECT h FROM DrawingHistory h WHERE h.ticketType = 'WINNING'")
    fun findAllWinners(): List<DrawingHistory>

}
