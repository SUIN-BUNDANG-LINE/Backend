package com.sbl.sulmun2yong.ai.dto.request

data class SurveyGenerationWithTextDocumentRequest(
    val job: String,
    val groupName: String,
    val textDocument: String,
    val userPrompt: String,
)
