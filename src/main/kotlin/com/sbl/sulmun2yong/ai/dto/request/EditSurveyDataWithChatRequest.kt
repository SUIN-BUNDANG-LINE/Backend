package com.sbl.sulmun2yong.ai.dto.request

import java.util.UUID

data class EditSurveyDataWithChatRequest(
    val surveyId: UUID,
    val modificationTargetId: UUID,
    val userPrompt: String,
)
