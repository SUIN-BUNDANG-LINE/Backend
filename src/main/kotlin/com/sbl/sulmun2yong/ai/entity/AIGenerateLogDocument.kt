package com.sbl.sulmun2yong.ai.entity

import com.sbl.sulmun2yong.ai.domain.AIGenerateLog
import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "aiGenerateLogs")
data class AIGenerateLogDocument(
    @Id
    val id: UUID,
    val surveyId: UUID,
    val makerId: UUID?,
    val userPrompt: String,
    val fileUrl: String?,
    val target: String,
    val groupName: String,
    val generatedSurvey: AIGeneratedSurvey,
    val visitorId: String?,
) : BaseTimeDocument() {
    companion object {
        fun from(aIGenerateLog: AIGenerateLog) =
            AIGenerateLogDocument(
                id = aIGenerateLog.id,
                surveyId = aIGenerateLog.surveyId,
                makerId = aIGenerateLog.makerId,
                userPrompt = aIGenerateLog.userPrompt,
                fileUrl = aIGenerateLog.fileUrl,
                target = aIGenerateLog.target,
                groupName = aIGenerateLog.groupName,
                generatedSurvey = aIGenerateLog.generatedSurvey,
                visitorId = aIGenerateLog.visitorId,
            )
    }

    fun toDomain() =
        AIGenerateLog(
            id = this.id,
            surveyId = this.surveyId,
            makerId = this.makerId,
            userPrompt = this.userPrompt,
            fileUrl = this.fileUrl,
            target = this.target,
            groupName = this.groupName,
            generatedSurvey = this.generatedSurvey,
            visitorId = this.visitorId,
        )
}
