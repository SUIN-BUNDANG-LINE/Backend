package com.sbl.sulmun2yong.ai.dto.python.request

import com.sbl.sulmun2yong.ai.domain.PythonFormattedSurvey
import java.util.UUID

data class EditSurveyRequestToPython(
    override val chatSessionId: UUID,
    override val userPrompt: String,
    override val isEditGeneratedResult: Boolean,
    val survey: PythonFormattedSurvey,
) : EditRequestToPython
