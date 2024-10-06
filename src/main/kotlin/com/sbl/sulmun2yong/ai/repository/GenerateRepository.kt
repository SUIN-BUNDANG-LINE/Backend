package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.dto.python.request.GenerateRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateWithFileUrlRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateWithTextDocumentRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.response.GenerateSurveyResponseFromPython
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class GenerateRepository(
    private val requestToPythonServerTemplate: RestTemplate,
) {
    fun requestWithFileUrl(generateWithFileUrlRequestToPython: GenerateWithFileUrlRequestToPython): GenerateSurveyResponseFromPython =
        requestGenerateSurvey(
            "/generate/survey/file-url",
            generateWithFileUrlRequestToPython,
        )

    fun requestWithTextDocument(
        generateWithTextDocumentRequestToPython: GenerateWithTextDocumentRequestToPython,
    ): GenerateSurveyResponseFromPython =
        requestGenerateSurvey(
            "/generate/survey/text-document",
            generateWithTextDocumentRequestToPython,
        )

    private fun requestGenerateSurvey(
        requestUrl: String,
        requestBody: GenerateRequestToPython,
    ): GenerateSurveyResponseFromPython =
        try {
            requestToPythonServerTemplate
                .postForEntity(
                    requestUrl,
                    requestBody,
                    GenerateSurveyResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }
}
