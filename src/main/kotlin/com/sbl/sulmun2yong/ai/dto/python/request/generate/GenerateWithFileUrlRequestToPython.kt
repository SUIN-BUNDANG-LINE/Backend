package com.sbl.sulmun2yong.ai.dto.python.request.generate

class GenerateWithFileUrlRequestToPython(
    override val job: String,
    override val groupName: String,
    override val userPrompt: String,
    val fileUrl: String,
) : GenerateRequestToPython
