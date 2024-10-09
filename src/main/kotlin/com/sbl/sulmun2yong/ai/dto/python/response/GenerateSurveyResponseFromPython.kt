package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.survey.domain.Survey
import java.util.UUID

data class GenerateSurveyResponseFromPython(
    val chatSessionId: UUID,
    val survey: SurveyResponseFromPython,
) {
    fun toDomain(originalSurvey: Survey) =
        AIGeneratedSurvey(
            chatSessionId = chatSessionId,
            survey = survey.toDomain().toNewSurvey(originalSurvey),
        )
}
