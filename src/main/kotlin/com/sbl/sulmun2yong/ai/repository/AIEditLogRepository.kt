package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.entity.AIEditLogDocument
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component
interface AIEditLogRepository : MongoRepository<AIEditLogDocument, UUID> {
    fun findFirstBySurveyIdAndMakerIdOrderByCreatedAtDesc(
        surveyId: UUID,
        makerId: UUID,
    ): Optional<AIEditLogDocument>
}
