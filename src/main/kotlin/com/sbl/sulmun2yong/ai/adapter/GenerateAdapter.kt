package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.AIGeneratedSurvey
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateWithFileUrlRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.GenerateWithTextDocumentRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.response.GenerateSurveyResponseFromPython
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import com.sbl.sulmun2yong.survey.domain.Survey
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class GenerateAdapter(
    private val requestToPythonServerTemplate: RestTemplate,
) {
    fun requestSurveyGenerationWithFileUrl(
        job: String,
        groupName: String,
        fileUrl: String,
        userPrompt: String,
        originalSurvey: Survey,
    ): AIGeneratedSurvey {
        val generateSurveyResponseFromPython =
            requestWithFileUrl(
                GenerateWithFileUrlRequestToPython(
                    job = job,
                    groupName = groupName,
                    userPrompt = userPrompt,
                    fileUrl = fileUrl,
                ),
            )

        return generateSurveyResponseFromPython.toDomain(originalSurvey)
    }

    fun requestSurveyGenerationWithTextDocument(
        job: String,
        groupName: String,
        textDocument: String,
        userPrompt: String,
        originalSurvey: Survey,
    ): AIGeneratedSurvey {
        val generateSurveyResponseFromPython =
            requestWithTextDocument(
                GenerateWithTextDocumentRequestToPython(
                    job = job,
                    groupName = groupName,
                    userPrompt = userPrompt,
                    textDocument = textDocument,
                ),
            )

        return generateSurveyResponseFromPython.toDomain(originalSurvey)
    }

    private fun requestWithFileUrl(
        generateWithFileUrlRequestToPython: GenerateWithFileUrlRequestToPython,
    ): GenerateSurveyResponseFromPython =
        requestGenerateSurvey(
            "/generate/survey/file-url",
            generateWithFileUrlRequestToPython,
        )

    private fun requestWithTextDocument(
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
