package com.sbl.sulmun2yong.ai.entity

import com.sbl.sulmun2yong.ai.domain.AIEditLog
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.entity.SurveyDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "aiEditLogs")
data class AIEditLogDocument(
    @Id
    val id: UUID,
    val surveyId: UUID,
    val makerId: UUID,
    val userPrompt: String,
    val originalSurvey: SurveyDocument,
    val editedSurvey: SurveyDocument,
) : BaseTimeDocument() {
    companion object {
        fun from(aiEditLog: AIEditLog) =
            AIEditLogDocument(
                id = aiEditLog.id,
                surveyId = aiEditLog.surveyId,
                makerId = aiEditLog.makerId,
                userPrompt = aiEditLog.userPrompt,
                originalSurvey = SurveyDocument.from(aiEditLog.originalSurvey),
                editedSurvey = SurveyDocument.from(aiEditLog.editedSurvey),
            )
    }

    fun toDomain() =
        AIEditLog(
            id = this.id,
            surveyId = this.surveyId,
            makerId = this.makerId,
            userPrompt = this.userPrompt,
            originalSurvey = this.originalSurvey.toDomain(),
            editedSurvey = this.editedSurvey.toDomain(),
        )
}
