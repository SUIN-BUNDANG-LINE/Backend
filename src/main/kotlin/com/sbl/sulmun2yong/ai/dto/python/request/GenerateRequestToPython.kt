package com.sbl.sulmun2yong.ai.dto.python.request

interface GenerateRequestToPython {
    val job: String
    val groupName: String
    val userPrompt: String
}
