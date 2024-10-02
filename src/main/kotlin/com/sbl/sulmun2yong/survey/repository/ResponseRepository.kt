package com.sbl.sulmun2yong.survey.repository

import com.sbl.sulmun2yong.survey.entity.ResponseDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ResponseRepository : MongoRepository<ResponseDocument, UUID> {
    fun findBySurveyId(surveyId: UUID): List<ResponseDocument>

    fun findBySurveyIdAndParticipantId(
        surveyId: UUID,
        participantId: UUID,
    ): List<ResponseDocument>
}
