package com.sbl.sulmun2yong.ai.dto.response

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import java.util.UUID

data class AISurveyGenerationResponse(
    val chatSessionId: UUID,
    val generatedSurvey: SurveyMakeInfoResponse,
) {
    companion object {
        fun of(aiGeneratedSurvey: AIGeneratedSurvey): AISurveyGenerationResponse =
            AISurveyGenerationResponse(aiGeneratedSurvey.chatSessionId, SurveyMakeInfoResponse.of(aiGeneratedSurvey.survey))
    }
}
