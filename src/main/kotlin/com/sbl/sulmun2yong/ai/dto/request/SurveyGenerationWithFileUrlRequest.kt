package com.sbl.sulmun2yong.ai.dto.request

data class SurveyGenerationWithFileUrlRequest(
    val target: String,
    val groupName: String,
    val fileUrl: String,
    val userPrompt: String,
)
