package com.sbl.sulmun2yong.ai.dto.response

import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import java.util.UUID

class SurveyGenerationResponse(
    val chatSessionId: UUID,
    val generatedSurvey: SurveyMakeInfoResponse,
) {
    companion object {
        fun from(
            chatSessionId: UUID,
            generatedSurvey: SurveyMakeInfoResponse,
        ): SurveyGenerationResponse = SurveyGenerationResponse(chatSessionId, generatedSurvey)
    }
}
