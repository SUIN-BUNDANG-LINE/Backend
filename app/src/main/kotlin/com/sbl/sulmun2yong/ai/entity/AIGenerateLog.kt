package com.sbl.sulmun2yong.ai.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.util.UUID

@Entity
@Table(name = "ai_generate_logs")
class AIGenerateLog(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @Column(columnDefinition = "BINARY(16)")
    val makerId: UUID?,
    @Column(nullable = false, columnDefinition = "TEXT")
    val userPrompt: String,
    @Column(length = 500)
    val fileUrl: String?,
    @Column(nullable = false)
    val target: String,
    @Column(nullable = false)
    val groupName: String,
    // AI 생성 결과를 JSON 문자열로 저장
    @Column(nullable = false, columnDefinition = "TEXT")
    val generatedSurveyJson: String,
    val visitorId: String?,
) : BaseTimeEntity() {
    @get:Transient
    val generatedSurvey: AIGeneratedSurvey
        get() = objectMapper.readValue(generatedSurveyJson, AIGeneratedSurvey::class.java)

    companion object {
        private val objectMapper = ObjectMapper().findAndRegisterModules()

        fun create(
            id: UUID,
            surveyId: UUID,
            makerId: UUID?,
            userPrompt: String,
            fileUrl: String?,
            target: String,
            groupName: String,
            generatedSurvey: AIGeneratedSurvey,
            visitorId: String?,
        ) = AIGenerateLog(
            id = id,
            surveyId = surveyId,
            makerId = makerId,
            userPrompt = userPrompt,
            fileUrl = fileUrl,
            target = target,
            groupName = groupName,
            generatedSurveyJson = objectMapper.writeValueAsString(generatedSurvey),
            visitorId = visitorId,
        )
    }
}
