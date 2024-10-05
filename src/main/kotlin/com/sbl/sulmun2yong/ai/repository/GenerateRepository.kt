package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.dto.python.request.generate.GenerateRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.generate.GenerateWithTextDocumentRequestToPython
import com.sbl.sulmun2yong.ai.entity.AISurveyGenerationResponseFromPython
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class GenerateRepository(
    private val requestToPythonTemplate: RestTemplate,
) {
    fun requestWithFileUrl(
        job: String,
        groupName: String,
        fileUrl: String,
        userPrompt: String,
    ) = requestGenerateSurvey(
        "/generate/survey/file-url",
        GenerateWithTextDocumentRequestToPython(
            job = job,
            groupName = groupName,
            textDocument = fileUrl,
            userPrompt = userPrompt,
        ),
    )

    fun requestWithTextDocument(
        job: String,
        groupName: String,
        textDocument: String,
        userPrompt: String,
    ) = requestGenerateSurvey(
        "/generate/survey/text-document",
        GenerateWithTextDocumentRequestToPython(
            job = job,
            groupName = groupName,
            textDocument = textDocument,
            userPrompt = userPrompt,
        ),
    )

    private fun requestGenerateSurvey(
        requestUrl: String,
        requestBody: GenerateRequestToPython,
    ): AISurveyGenerationResponseFromPython =
        try {
            requestToPythonTemplate
                .postForEntity(
                    requestUrl,
                    requestBody,
                    AISurveyGenerationResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }
}
