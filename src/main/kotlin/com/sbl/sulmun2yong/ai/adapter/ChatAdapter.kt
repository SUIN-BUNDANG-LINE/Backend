package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.PythonFormattedQuestion
import com.sbl.sulmun2yong.ai.domain.PythonFormattedSection
import com.sbl.sulmun2yong.ai.domain.PythonFormattedSurvey
import com.sbl.sulmun2yong.ai.dto.python.request.EditQuestionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSectionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSurveyRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.response.QuestionResponseFromPython
import com.sbl.sulmun2yong.ai.dto.python.response.SectionResponseFromPython
import com.sbl.sulmun2yong.ai.dto.python.response.SurveyResponseFromPython
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
class ChatAdapter(
    private val requestToPythonServerTemplate: RestTemplate,
) {
    fun requestEditSurveyWithChat(
        chatSessionId: UUID,
        survey: Survey,
        userPrompt: String,
    ): PythonFormattedSurvey =
        requestEditSurvey(
            EditSurveyRequestToPython(
                chatSessionId = chatSessionId,
                survey = PythonFormattedSurvey.from(survey),
                userPrompt = userPrompt,
            ),
        ).toDomain()

    fun requestEditSectionWithChat(
        chatSessionId: UUID,
        section: Section,
        userPrompt: String,
    ) = requestEditSection(
        EditSectionRequestToPython(
            chatSessionId = chatSessionId,
            section = PythonFormattedSection.from(section),
            userPrompt = userPrompt,
        ),
    ).toDomain()

    fun requestEditQuestionWithChat(
        chatSessionId: UUID,
        question: Question,
        userPrompt: String,
    ) = requestEditQuestion(
        EditQuestionRequestToPython(
            chatSessionId = chatSessionId,
            question = PythonFormattedQuestion.from(question),
            userPrompt = userPrompt,
        ),
    ).toDomain()

    private fun requestEditSurvey(editSurveyRequestToPython: EditSurveyRequestToPython): SurveyResponseFromPython =
        runCatching {
            requestToPythonServerTemplate
                .postForEntity(
                    "/chat/edit/survey",
                    editSurveyRequestToPython,
                    SurveyResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                if (throwable is HttpClientErrorException) {
                    throw PythonServerExceptionMapper.mapException(throwable)
                } else {
                    throw SurveyGenerationByAIFailedException()
                }
            },
        )

    private fun requestEditSection(editSectionRequestToPython: EditSectionRequestToPython): SectionResponseFromPython =
        runCatching {
            requestToPythonServerTemplate
                .postForEntity(
                    "/chat/edit/section",
                    editSectionRequestToPython,
                    SectionResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                if (throwable is HttpClientErrorException) {
                    throw PythonServerExceptionMapper.mapException(throwable)
                } else {
                    throw SurveyGenerationByAIFailedException()
                }
            },
        )

    private fun requestEditQuestion(editQuestionRequestToPython: EditQuestionRequestToPython): QuestionResponseFromPython =
        runCatching {
            requestToPythonServerTemplate
                .postForEntity(
                    "/chat/edit/question",
                    editQuestionRequestToPython,
                    QuestionResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                if (throwable is HttpClientErrorException) {
                    throw PythonServerExceptionMapper.mapException(throwable)
                } else {
                    throw SurveyGenerationByAIFailedException()
                }
            },
        )
}
