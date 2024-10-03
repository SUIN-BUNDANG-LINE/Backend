package com.sbl.sulmun2yong.ai.dto.request

import java.util.UUID

class EditSurveyDataWithChatRequest(
    val surveyId: UUID,
    val modificationTargetId: UUID,
    val userPrompt: String,
)
