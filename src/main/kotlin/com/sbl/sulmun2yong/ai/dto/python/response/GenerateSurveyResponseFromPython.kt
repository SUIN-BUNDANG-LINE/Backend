package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.survey.domain.Survey

data class GenerateSurveyResponseFromPython(
    val survey: SurveyResponseFromPython,
) {
    fun toDomain(originalSurvey: Survey) =
        AIGeneratedSurvey(
            survey = survey.toDomain().toNewSurvey(originalSurvey),
        )
}
