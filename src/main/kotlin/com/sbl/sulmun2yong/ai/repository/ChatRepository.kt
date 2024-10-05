package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.dto.python.request.edit.EditQuestionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.edit.EditSectionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.edit.EditSurveyRequestToPython
import com.sbl.sulmun2yong.ai.entity.PythonServerQuestionFormat
import com.sbl.sulmun2yong.ai.entity.PythonServerSectionFormat
import com.sbl.sulmun2yong.ai.entity.PythonServerSurveyFormat
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.section.Section
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class ChatRepository(
    private val requestToPythonTemplate: RestTemplate,
) {
    fun requestEditSurvey(
        chatSessionId: UUID,
        survey: Survey,
        userPrompt: String,
    ): PythonServerSurveyFormat {
        val requestUrl = "/chat/edit/survey"
        val requestBody =
            EditSurveyRequestToPython(
                chatSessionId = chatSessionId,
                survey = PythonServerSurveyFormat.of(survey),
                userPrompt = userPrompt,
            )
        return try {
            requestToPythonTemplate
                .postForEntity(
                    requestUrl,
                    requestBody,
                    PythonServerSurveyFormat::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }
    }

    fun requestEditSection(
        chatSessionId: UUID,
        section: Section,
        userPrompt: String,
    ): PythonServerSectionFormat {
        val requestUrl = "/chat/edit/section"
        val requestBody =
            EditSectionRequestToPython(
                chatSessionId = chatSessionId,
                section = PythonServerSectionFormat.of(section),
                userPrompt = userPrompt,
            )
        return try {
            requestToPythonTemplate
                .postForEntity(
                    requestUrl,
                    requestBody,
                    PythonServerSectionFormat::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }
    }

    fun requestEditQuestion(
        chatSessionId: UUID,
        question: Question,
        userPrompt: String,
    ): PythonServerQuestionFormat {
        val requestUrl = "/chat/edit/question"
        val requestBody =
            EditQuestionRequestToPython(
                chatSessionId = chatSessionId,
                question = PythonServerQuestionFormat.of(question),
                userPrompt = userPrompt,
            )
        return try {
            requestToPythonTemplate
                .postForEntity(
                    requestUrl,
                    requestBody,
                    PythonServerQuestionFormat::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }
    }
}
