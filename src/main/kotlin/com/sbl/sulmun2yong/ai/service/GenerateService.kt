package com.sbl.sulmun2yong.ai.service

import com.sbl.sulmun2yong.ai.adapter.GenerateAdapter
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithFileUrlRequest
import com.sbl.sulmun2yong.ai.dto.request.SurveyGenerationWithTextDocumentRequest
import com.sbl.sulmun2yong.ai.dto.response.AISurveyGenerationResponse
import com.sbl.sulmun2yong.global.util.validator.FileUrlValidator
import com.sbl.sulmun2yong.survey.adapter.SurveyAdapter
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GenerateService(
    private val fileUrlValidator: FileUrlValidator,
    private val generateAdapter: GenerateAdapter,
    private val surveyAdapter: SurveyAdapter,
) {
    fun generateSurveyWithFileUrl(
        surveyGenerationWithFileUrlRequest: SurveyGenerationWithFileUrlRequest,
        surveyId: UUID,
    ): AISurveyGenerationResponse {
        val allowedExtensions = mutableListOf(".txt", ".pdf", ".pptx", ".docx")

        val target = surveyGenerationWithFileUrlRequest.target
        val groupName = surveyGenerationWithFileUrlRequest.groupName
        val fileUrl = surveyGenerationWithFileUrlRequest.fileUrl
        val userPrompt = surveyGenerationWithFileUrlRequest.userPrompt

        fileUrlValidator.validateFileUrlOf(fileUrl, allowedExtensions)

        val survey = surveyAdapter.getSurvey(surveyId)

        return AISurveyGenerationResponse.from(
            generateAdapter.requestSurveyGenerationWithFileUrl(surveyId, target, groupName, fileUrl, userPrompt, survey),
        )
    }

    fun generateSurveyWithTextDocument(
        surveyGenerationWithTextDocumentRequest: SurveyGenerationWithTextDocumentRequest,
        surveyId: UUID,
    ): AISurveyGenerationResponse {
        val target = surveyGenerationWithTextDocumentRequest.target
        val groupName = surveyGenerationWithTextDocumentRequest.groupName
        val textDocument = surveyGenerationWithTextDocumentRequest.textDocument
        val userPrompt = surveyGenerationWithTextDocumentRequest.userPrompt

        val survey = surveyAdapter.getSurvey(surveyId)

        return AISurveyGenerationResponse.from(
            generateAdapter.requestSurveyGenerationWithTextDocument(surveyId, target, groupName, textDocument, userPrompt, survey),
        )
    }
}
