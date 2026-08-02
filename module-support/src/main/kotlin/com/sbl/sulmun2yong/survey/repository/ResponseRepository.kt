package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.entity.ResponseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ResponseRepository : JpaRepository<ResponseEntity, UUID> {
    fun findBySurveyId(surveyId: UUID): List<ResponseEntity>

    fun findBySurveyIdAndParticipantId(
        surveyId: UUID,
        participantId: UUID,
    ): List<ResponseEntity>
}
