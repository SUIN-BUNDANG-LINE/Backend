package com.sbl.sulmun2yong.ai.dto.python.request

import java.util.UUID

data class GenerateWithTextDocumentRequestToPython(
    override val chatSessionId: UUID,
    override val target: String,
    override val groupName: String,
    override val userPrompt: String,
    val textDocument: String,
) : GenerateRequestToPython
