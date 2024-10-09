package com.sbl.sulmun2yong.ai.dto.python.request

import com.sbl.sulmun2yong.ai.domain.PythonFormattedSurvey
import java.util.UUID

data class EditSurveyRequestToPython(
    override val chatSessionId: UUID,
    override val userPrompt: String,
    val survey: PythonFormattedSurvey,
) : EditRequestToPython
