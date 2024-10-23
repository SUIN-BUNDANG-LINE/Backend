package com.sbl.sulmun2yong.ai.dto.response

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse

data class AISurveyGenerationResponse(
    val generatedSurvey: SurveyMakeInfoResponse,
) {
    companion object {
        fun from(aiGeneratedSurvey: AIGeneratedSurvey): AISurveyGenerationResponse =
            AISurveyGenerationResponse(SurveyMakeInfoResponse.of(aiGeneratedSurvey.survey))
    }
}
