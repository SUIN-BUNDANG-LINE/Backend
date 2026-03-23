package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.entity.ParticipantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ParticipantRepository : JpaRepository<ParticipantEntity, UUID> {
    fun findBySurveyId(surveyId: UUID): List<ParticipantEntity>

    fun findBySurveyIdAndVisitorId(
        surveyId: UUID,
        visitorId: String,
    ): Optional<ParticipantEntity>
}
