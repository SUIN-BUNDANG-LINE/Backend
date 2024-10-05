package com.sbl.sulmun2yong.ai.entity

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import java.util.UUID

data class AISurveyGenerationResponseFromPython(
    val chatSessionId: UUID,
    val pythonServerSurveyFormat: PythonServerSurveyFormat,
) {
    fun toDomain() =
        AIGeneratedSurvey(
            chatSessionId = chatSessionId,
            survey = pythonServerSurveyFormat.toDomain(),
        )
}
