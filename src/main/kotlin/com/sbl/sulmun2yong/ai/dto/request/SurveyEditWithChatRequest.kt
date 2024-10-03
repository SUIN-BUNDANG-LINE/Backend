package com.sbl.sulmun2yong.ai.dto.request

import java.util.UUID

class SurveyEditWithChatRequest(
    val surveyId: UUID,
    val surveyDataId: UUID,
    val surveyDataType: String,
    val userPrompt: String,
)
