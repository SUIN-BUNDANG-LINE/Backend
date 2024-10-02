package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.entity.ParticipantDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ParticipantRepository : MongoRepository<ParticipantDocument, UUID> {
    fun findBySurveyId(surveyId: UUID): List<ParticipantDocument>

    fun findBySurveyIdAndVisitorId(
        surveyId: UUID,
        visitorId: String,
    ): Optional<ParticipantDocument>
}
