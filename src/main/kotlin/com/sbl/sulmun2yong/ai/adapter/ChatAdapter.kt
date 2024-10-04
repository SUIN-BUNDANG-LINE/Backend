package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.dto.PythonServerQuestionFormat
import com.sbl.sulmun2yong.ai.dto.PythonServerSectionFormat
import com.sbl.sulmun2yong.ai.dto.PythonServerSurveyFormat
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.section.Section
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
        chatSessionId: UUID,
        survey: Survey,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/chat/edit/survey"

        val requestBody =
            mapOf(
                "chat_session_id" to chatSessionId,
                "survey" to PythonServerSurveyFormat.of(survey),
                "user_prompt" to userPrompt,
            )

        return requestEditWithChat(requestUrl, requestBody)
    }

    fun requestEditSectionWithChat(
        chatSessionId: UUID,
        section: Section,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/chat/edit/section"

        val requestBody =
            mapOf(
                "chat_session_id" to chatSessionId,
                "section" to PythonServerSectionFormat.of(section),
                "user_prompt" to userPrompt,
            )

        return requestEditWithChat(requestUrl, requestBody)
    }

    fun requestEditQuestionWithChat(
        chatSessionId: UUID,
        question: Question,
        userPrompt: String,
    ): SurveyMakeInfoResponse {
        val requestUrl = "$aiServerBaseUrl/chat/edit/question"

        val requestBody =
            mapOf(
                "chat_session_id" to chatSessionId,
                "question" to PythonServerQuestionFormat.of(question),
                "user_prompt" to userPrompt,
            )

        return requestEditWithChat(requestUrl, requestBody)
    }

    private fun requestEditWithChat(
        requestUrl: String,
        requestBody: Map<String, Any>,
    ): SurveyMakeInfoResponse {
        val surveyGeneratedByAI =
            try {
                restTemplate
                    .postForEntity(
                        requestUrl,
                        requestBody,
                        PythonServerSurveyFormat::class.java,
                    ).body ?: throw SurveyGenerationByAIFailedException()
            } catch (e: HttpClientErrorException) {
                throw PythonServerExceptionMapper.mapException(e)
            }

        val survey = surveyGeneratedByAI.toDomain()
        return SurveyMakeInfoResponse.of(survey)
    }
}
