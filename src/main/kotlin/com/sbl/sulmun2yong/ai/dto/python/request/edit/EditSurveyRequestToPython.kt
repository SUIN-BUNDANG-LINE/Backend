package com.sbl.sulmun2yong.ai.dto.python.request.edit

import com.sbl.sulmun2yong.ai.entity.PythonServerSurveyFormat
import java.util.UUID

class EditSurveyRequestToPython(
    override val chatSessionId: UUID,
    override val userPrompt: String,
    val survey: PythonServerSurveyFormat,
) : EditRequestToPython
