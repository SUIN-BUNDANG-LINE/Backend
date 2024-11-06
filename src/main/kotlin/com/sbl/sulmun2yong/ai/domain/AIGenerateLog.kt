package com.sbl.sulmun2yong.ai.domain

import java.util.UUID

class AIGenerateLog(
    val id: UUID,
    val surveyId: UUID,
    val makerId: UUID?,
    val userPrompt: String,
    val fileUrl: String?,
    val target: String,
    val groupName: String,
    val generatedSurvey: AIGeneratedSurvey,
    val visitorId: String?,
)
