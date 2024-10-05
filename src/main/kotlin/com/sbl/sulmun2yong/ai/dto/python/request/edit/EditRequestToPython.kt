package com.sbl.sulmun2yong.ai.dto.python.request.edit

import java.util.UUID

interface EditRequestToPython {
    val chatSessionId: UUID
    val userPrompt: String
}
