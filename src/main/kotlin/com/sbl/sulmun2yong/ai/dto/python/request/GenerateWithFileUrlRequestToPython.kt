package com.sbl.sulmun2yong.ai.dto.python.request

import java.util.UUID

data class GenerateWithFileUrlRequestToPython(
    override val chatSessionId: UUID,
    override val job: String,
    override val groupName: String,
    override val userPrompt: String,
    val fileUrl: String,
) : GenerateRequestToPython
