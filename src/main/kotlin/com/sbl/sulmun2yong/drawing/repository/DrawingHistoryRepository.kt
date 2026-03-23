package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingHistoryRepository : JpaRepository<DrawingHistoryEntity, UUID> {
    @Query(
        "SELECT h FROM DrawingHistoryEntity h WHERE h.surveyId = :surveyId " +
            "AND (h.participantId = :participantId OR h.phoneNumber = :phoneNumber)",
    )
    fun findBySurveyIdAndParticipantIdOrPhoneNumber(
        @Param("surveyId") surveyId: UUID,
        @Param("participantId") participantId: UUID,
        @Param("phoneNumber") phoneNumber: String,
    ): Optional<DrawingHistoryEntity>

    fun findBySurveyId(surveyId: UUID): List<DrawingHistoryEntity>

    @Query("SELECT h FROM DrawingHistoryEntity h WHERE h.surveyId = :surveyId AND h.ticketType = 'WINNING'")
    fun findBySurveyIdForWinner(
        @Param("surveyId") surveyId: UUID,
    ): List<DrawingHistoryEntity>

    @Query("SELECT h FROM DrawingHistoryEntity h WHERE h.ticketType = 'WINNING'")
    fun findAllWinners(): List<DrawingHistoryEntity>

    fun deleteBySurveyId(surveyId: UUID)
}
