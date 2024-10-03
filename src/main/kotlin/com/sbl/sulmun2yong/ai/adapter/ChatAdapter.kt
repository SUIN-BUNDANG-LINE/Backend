package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.dto.ChatSessionIdWithSurveyGeneratedByAI
import com.sbl.sulmun2yong.ai.dto.response.AISurveyGenerationResponse
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import com.sbl.sulmun2yong.survey.dto.response.SurveyMakeInfoResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class ChatAdapter(
    @Value("\${ai-server.base-url}")
    private val aiServerBaseUrl: String,
    private val restTemplate: RestTemplate,
) {
    fun requestEditSurveyWithChat(
        surveyId: UUID,
        modificationTargetId: UUID,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/chat/edit"

        val requestBody =
            mapOf(
                "survey_id" to surveyId,
                "modification_target_id" to modificationTargetId,
                "user_prompt" to userPrompt,
            )

        return requestEditWithChat(requestUrl, requestBody)
    }

    fun requestEditSectionWithChat(
        surveyId: UUID,
        modificationTargetId: UUID,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/chat/edit"

        val requestBody =
            mapOf(
                "survey_id" to surveyId,
                "modification_target_id" to modificationTargetId,
                "user_prompt" to userPrompt,
            )

        return requestToEditSurveyWithChat(requestUrl, requestBody)
    }

    fun requestEditQuestionWithChat(
        surveyId: UUID,
        modificationTargetId: UUID,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/chat/edit"

        val requestBody =
            mapOf(
                "survey_id" to surveyId,
                "modification_target_id" to modificationTargetId,
                "user_prompt" to userPrompt,
            )

        return requestToEditSurveyWithChat(requestUrl, requestBody)
    }

    private fun requestEditWithChat(
        requestUrl: String,
        requestBody: Map<String, String>,
    ): AISurveyGenerationResponse {
        val chatSessionIdWithSurveyGeneratedByAI =
            try {
                restTemplate
                    .postForEntity(
                        requestUrl,
                        requestBody,
                        ChatSessionIdWithSurveyGeneratedByAI::class.java,
                    ).body ?: throw SurveyGenerationByAIFailedException()
            } catch (e: HttpClientErrorException) {
                throw PythonServerExceptionMapper.mapException(e)
            }

        val chatSessionId = chatSessionIdWithSurveyGeneratedByAI.chatSessionId
        val survey = chatSessionIdWithSurveyGeneratedByAI.surveyGeneratedByAI.toDomain()
        val surveyMakeInfoResponse = SurveyMakeInfoResponse.of(survey)
        return AISurveyGenerationResponse.from(chatSessionId, surveyMakeInfoResponse)
    }
}
