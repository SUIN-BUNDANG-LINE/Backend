package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.dto.SurveyGeneratedByAI
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class GenerateAdapter(
    @Value("\${ai-server.base-url}")
    private val aiServerBaseUrl: String,
    private val restTemplate: RestTemplate,
) {
    fun requestSurveyGenerationWithFileUrl(
        job: String,
        groupName: String,
        fileUrl: String,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/generate/survey/file-url"

        val requestBody =
            mapOf(
                "job" to job,
                "group_name" to groupName,
                "file_url" to fileUrl,
                "user_prompt" to userPrompt,
            )

        return requestToGenerateSurvey(requestUrl, requestBody)
    }

    fun requestSurveyGenerationWithTextDocument(
        job: String,
        groupName: String,
        textDocument: String,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/generate/survey/text-document"

        val requestBody =
            mapOf(
                "job" to job,
                "group_name" to groupName,
                "text_document" to textDocument,
                "user_prompt" to userPrompt,
            )

        return requestToGenerateSurvey(requestUrl, requestBody)
    }

    private fun requestToGenerateSurvey(
        requestUrl: String,
        requestBody: Map<String, String>,
    ): SurveyMakeInfoResponse {
        val surveyGeneratedByAI =
            try {
                restTemplate
                    .postForEntity(
                        requestUrl,
                        requestBody,
                        SurveyGeneratedByAI::class.java,
                    ).body ?: throw SurveyGenerationByAIFailedException()
            } catch (e: HttpClientErrorException) {
                throw PythonServerExceptionMapper.mapException(e)
            }

        val survey = surveyGeneratedByAI.toDomain()
        return SurveyMakeInfoResponse.of(survey)
    }
}
