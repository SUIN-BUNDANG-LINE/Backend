package com.sbl.sulmun2yong.ai.repository

import com.sbl.sulmun2yong.ai.dto.python.request.EditQuestionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSectionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSurveyRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.response.QuestionResponseFromPython
import com.sbl.sulmun2yong.ai.dto.python.response.SectionResponseFromPython
import com.sbl.sulmun2yong.ai.dto.python.response.SurveyResponseFromPython
import com.sbl.sulmun2yong.ai.exception.SurveyGenerationByAIFailedException
import com.sbl.sulmun2yong.global.error.PythonServerExceptionMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class ChatRepository(
    private val requestToPythonServerTemplate: RestTemplate,
) {
    fun requestEditSurvey(editSurveyRequestToPython: EditSurveyRequestToPython): SurveyResponseFromPython =
        try {
            requestToPythonServerTemplate
                .postForEntity(
                    "/chat/edit/survey",
                    editSurveyRequestToPython,
                    SurveyResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }

    fun requestEditSection(editSectionRequestToPython: EditSectionRequestToPython): SectionResponseFromPython =
        try {
            requestToPythonServerTemplate
                .postForEntity(
                    "/chat/edit/section",
                    editSectionRequestToPython,
                    SectionResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }

    fun requestEditQuestion(editQuestionRequestToPython: EditQuestionRequestToPython): QuestionResponseFromPython =
        try {
            requestToPythonServerTemplate
                .postForEntity(
                    "/chat/edit/question",
                    editQuestionRequestToPython,
                    QuestionResponseFromPython::class.java,
                ).body ?: throw SurveyGenerationByAIFailedException()
        } catch (e: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(e)
        }
}
