package com.sbl.sulmun2yong.ai.dto.python.request.generate

class GenerateWithTextDocumentRequestToPython(
    override val job: String,
    override val groupName: String,
    override val userPrompt: String,
    val textDocument: String,
) : GenerateRequestToPython
