package com.sbl.sulmun2yong.ai.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.ai.domain.AIGenerateLog
import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "ai_generate_logs")
class AIGenerateLogEntity(
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
    val generatedSurvey: String,
    val visitorId: String?,
) : BaseTimeEntity() {
    companion object {
        private val objectMapper = ObjectMapper().findAndRegisterModules()

        fun from(aiGenerateLog: AIGenerateLog) =
            AIGenerateLogEntity(
                id = aiGenerateLog.id,
                surveyId = aiGenerateLog.surveyId,
                makerId = aiGenerateLog.makerId,
                userPrompt = aiGenerateLog.userPrompt,
                fileUrl = aiGenerateLog.fileUrl,
                target = aiGenerateLog.target,
                groupName = aiGenerateLog.groupName,
                generatedSurvey = objectMapper.writeValueAsString(aiGenerateLog.generatedSurvey),
                visitorId = aiGenerateLog.visitorId,
            )
    }

    fun toDomain() =
        AIGenerateLog(
            id = id,
            surveyId = surveyId,
            makerId = makerId,
            userPrompt = userPrompt,
            fileUrl = fileUrl,
            target = target,
            groupName = groupName,
            generatedSurvey = objectMapper.readValue(generatedSurvey, AIGeneratedSurvey::class.java),
            visitorId = visitorId,
        )
}
