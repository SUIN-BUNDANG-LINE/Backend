package com.sbl.sulmun2yong.ai.dto.request

data class SurveyGenerationWithTextDocumentRequest(
    val target: String,
    val groupName: String,
    val textDocument: String?,
    val userPrompt: String,
)
