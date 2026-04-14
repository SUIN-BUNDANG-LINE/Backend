package com.sbl.sulmun2yong.drawing.repository

import com.sbl.sulmun2yong.drawing.entity.DrawingHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DrawingHistoryRepository : JpaRepository<DrawingHistory, UUID> {
    @Query(
        value =
            "SELECT * FROM drawing_histories h WHERE h.survey_id = :surveyId " +
                "AND (h.participant_id = :participantId OR h.phone_number = :phoneNumber) LIMIT 1",
        nativeQuery = true,
    )
    fun findBySurveyIdAndParticipantIdOrPhoneNumber(
        @Param("surveyId") surveyId: UUID,
        @Param("participantId") participantId: UUID,
        @Param("phoneNumber") phoneNumber: String,
    ): Optional<DrawingHistory>

    fun findBySurveyId(surveyId: UUID): List<DrawingHistory>

    @Query("SELECT h FROM DrawingHistory h WHERE h.surveyId = :surveyId AND h.ticketType = 'WINNING'")
    fun findBySurveyIdForWinner(
        @Param("surveyId") surveyId: UUID,
    ): List<DrawingHistory>

    @Query("SELECT h FROM DrawingHistory h WHERE h.ticketType = 'WINNING'")
    fun findAllWinners(): List<DrawingHistory>

    fun deleteBySurveyId(surveyId: UUID)
}
