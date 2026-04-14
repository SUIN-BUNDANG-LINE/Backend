package com.sbl.sulmun2yong.ai.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.survey.entity.Survey
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.util.UUID

@Entity
@Table(name = "ai_edit_logs")
class AIEditLog(
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
    val originalSurveyJson: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val editedSurveyJson: String,
) : BaseTimeEntity() {
    @get:Transient
    val originalSurvey: Survey
        get() = objectMapper.readValue(originalSurveyJson, Survey::class.java)

    @get:Transient
    val editedSurvey: Survey
        get() = objectMapper.readValue(editedSurveyJson, Survey::class.java)

    companion object {
        private val objectMapper = ObjectMapper().findAndRegisterModules()

        fun create(
            id: UUID,
            surveyId: UUID,
            makerId: UUID,
            userPrompt: String,
            originalSurvey: Survey,
            editedSurvey: Survey,
        ) = AIEditLog(
            id = id,
            surveyId = surveyId,
            makerId = makerId,
            userPrompt = userPrompt,
            originalSurveyJson = objectMapper.writeValueAsString(originalSurvey),
            editedSurveyJson = objectMapper.writeValueAsString(editedSurvey),
        )
    }
}
