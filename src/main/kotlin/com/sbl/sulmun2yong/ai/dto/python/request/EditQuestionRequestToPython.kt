package com.sbl.sulmun2yong.ai.dto.python.request

import com.sbl.sulmun2yong.ai.domain.PythonFormattedQuestion
import java.util.UUID

data class EditQuestionRequestToPython(
    override val chatSessionId: UUID,
    override val userPrompt: String,
    val question: PythonFormattedQuestion,
) : EditRequestToPython
