package com.sbl.sulmun2yong.ai.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.ai.domain.AIEditLog
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.survey.domain.Survey
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "ai_edit_logs")
class AIEditLogEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val makerId: UUID,
    @Column(nullable = false, columnDefinition = "TEXT")
    val userPrompt: String,
    // 편집 전후 설문 스냅샷을 JSON으로 저장
    @Column(nullable = false, columnDefinition = "TEXT")
    val originalSurvey: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val editedSurvey: String,
) : BaseTimeEntity() {
    companion object {
        private val objectMapper = ObjectMapper().findAndRegisterModules()

        fun from(aiEditLog: AIEditLog) =
            AIEditLogEntity(
                id = aiEditLog.id,
                surveyId = aiEditLog.surveyId,
                makerId = aiEditLog.makerId,
                userPrompt = aiEditLog.userPrompt,
                originalSurvey = objectMapper.writeValueAsString(aiEditLog.originalSurvey),
                editedSurvey = objectMapper.writeValueAsString(aiEditLog.editedSurvey),
            )
    }

    fun toDomain() =
        AIEditLog(
            id = id,
            surveyId = surveyId,
            makerId = makerId,
            userPrompt = userPrompt,
            originalSurvey = objectMapper.readValue(originalSurvey, Survey::class.java),
            editedSurvey = objectMapper.readValue(editedSurvey, Survey::class.java),
        )
}
