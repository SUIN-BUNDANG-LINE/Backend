package com.sbl.sulmun2yong.ai.dto.python.request.edit

import com.sbl.sulmun2yong.ai.entity.PythonServerQuestionFormat
import java.util.UUID

class EditQuestionRequestToPython(
    override val chatSessionId: UUID,
    override val userPrompt: String,
    val question: PythonServerQuestionFormat,
) : EditRequestToPython
