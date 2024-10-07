package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import java.util.UUID

data class GenerateSurveyResponseFromPython(
    val chatSessionId: UUID,
    val survey: SurveyResponseFromPython,
) {
    fun toDomain() =
        AIGeneratedSurvey(
            chatSessionId = chatSessionId,
            survey = survey.toDomain().toNewSurvey(),
        )
}
