package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.entity.Participant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ParticipantRepository : JpaRepository<Participant, UUID> {
    fun findBySurveyId(surveyId: UUID): List<Participant>

    fun findBySurveyIdAndVisitorId(
        surveyId: UUID,
        visitorId: String,
    ): Optional<Participant>
}
