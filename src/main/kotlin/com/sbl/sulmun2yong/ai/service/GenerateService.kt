package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.GenerateAdapter
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithTextDocumentRequest
import com.sbl.sulmun2yong.ai.dto.response.AISurveyGenerationResponse
import com.sbl.sulmun2yong.global.util.validator.FileUrlValidator
import org.springframework.stereotype.Service

@Service
class GenerateService(
    private val fileUrlValidator: FileUrlValidator,
    private val generateAdapter: GenerateAdapter,
) {
    fun generateSurveyWithFileUrl(surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest): AISurveyGenerationResponse {
        val allowedExtensions = mutableListOf(".txt", ".pdf")

        val job = surveyGenerationWithFileUrlRequest.job
        val groupName = surveyGenerationWithFileUrlRequest.groupName
        val fileUrl = surveyGenerationWithFileUrlRequest.fileUrl
        val userPrompt = surveyGenerationWithFileUrlRequest.userPrompt

        fileUrlValidator.validateFileUrlOf(fileUrl, allowedExtensions)

        return generateAdapter.requestSurveyGenerationWithFileUrl(job, groupName, fileUrl, userPrompt)
    }

    fun generateSurveyWithTextDocument(
        surveyGenerationWithTextDocumentRequest: SurveyGenerationWithTextDocumentRequest,
    ): AISurveyGenerationResponse {
        val job = surveyGenerationWithTextDocumentRequest.job
        val groupName = surveyGenerationWithTextDocumentRequest.groupName
        val textDocument = surveyGenerationWithTextDocumentRequest.textDocument
        val userPrompt = surveyGenerationWithTextDocumentRequest.userPrompt

        return generateAdapter.requestSurveyGenerationWithTextDocument(job, groupName, textDocument, userPrompt)
    }
}
