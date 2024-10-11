package com.sbl.sulmun2yong.ai.dto.python.request

import java.util.UUID

interface GenerateRequestToPython {
    val chatSessionId: UUID
    val job: String
    val groupName: String
    val userPrompt: String
}
