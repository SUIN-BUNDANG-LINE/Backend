package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.GenerateAdapter
import com.sbl.sulmun2yong.ai.dto.response.SurveyGenerationResponse
import com.sbl.sulmun2yong.global.util.validator.FileUrlValidator
import org.springframework.stereotype.Service

@Service
class GenerateService(
    private val fileUrlValidator: FileUrlValidator,
    private val generateAdapter: GenerateAdapter,
) {
    fun generateSurveyWithFileUrl(
        job: String,
        groupName: String,
        fileUrl: String,
        userPrompt: String,
    ): SurveyGenerationResponse {
        val allowedExtensions = mutableListOf(".txt", ".pdf")
        fileUrlValidator.validateFileUrlOf(fileUrl, allowedExtensions)

        return generateAdapter.requestSurveyGenerationWithFileUrl(job, groupName, fileUrl, userPrompt)
    }

    fun generateSurveyWithTextDocument(
        job: String,
        groupName: String,
        textDocument: String,
        userPrompt: String,
    ): SurveyGenerationResponse = generateAdapter.requestSurveyGenerationWithTextDocument(job, groupName, textDocument, userPrompt)
}
