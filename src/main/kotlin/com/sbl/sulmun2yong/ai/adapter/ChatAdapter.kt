package com.sbl.sulmun2yong.ai.adapter

import com.sbl.sulmun2yong.ai.domain.PythonFormattedQuestion
import com.sbl.sulmun2yong.ai.domain.PythonFormattedSection
import com.sbl.sulmun2yong.ai.domain.PythonFormattedSurvey
import com.sbl.sulmun2yong.ai.dto.python.request.EditQuestionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSectionRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.request.EditSurveyRequestToPython
import com.sbl.sulmun2yong.ai.dto.python.response.QuestionResponseFromPython
import com.sbl.sulmun2yong.ai.dto.python.response.SectionResponseFromPython
import com.sbl.sulmun2yong.ai.dto.python.response.SurveyResponseFromPython
import com.sbl.sulmun2yong.ai.exception.SurveyAIProcessingFailedException
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
    companion object {
        private const val EDIT_SURVEY_URL = "/chat/edit/survey"
        private const val EDIT_SECTION_URL = "/chat/edit/section"
        private const val EDIT_QUESTION_URL = "/chat/edit/question"
    }

    /** 설문 전체를 편집 요청하는 메서드 */
    fun requestEditSurveyWithChat(
        chatSessionId: UUID,
        survey: Survey,
        userPrompt: String,
    ): PythonFormattedSurvey =
        sendRequest(
            url = EDIT_SURVEY_URL,
            requestBody =
                EditSurveyRequestToPython(
                    chatSessionId = chatSessionId,
                    survey = PythonFormattedSurvey.from(survey),
                    userPrompt = userPrompt,
                ),
            responseType = SurveyResponseFromPython::class.java,
        ).toDomain()

    /** 섹션을 편집 요청하는 메서드 */
    fun requestEditSectionWithChat(
        chatSessionId: UUID,
        section: Section,
        userPrompt: String,
    ): PythonFormattedSection =
        sendRequest(
            url = EDIT_SECTION_URL,
            requestBody =
                EditSectionRequestToPython(
                    chatSessionId = chatSessionId,
                    section = PythonFormattedSection.from(section),
                    userPrompt = userPrompt,
                ),
            responseType = SectionResponseFromPython::class.java,
        ).toDomain()

    /** 질문을 편집 요청하는 메서드 */
    fun requestEditQuestionWithChat(
        chatSessionId: UUID,
        question: Question,
        userPrompt: String,
    ): PythonFormattedQuestion =
        sendRequest(
            url = EDIT_QUESTION_URL,
            requestBody =
                EditQuestionRequestToPython(
                    chatSessionId = chatSessionId,
                    question = PythonFormattedQuestion.from(question),
                    userPrompt = userPrompt,
                ),
            responseType = QuestionResponseFromPython::class.java,
        ).toDomain()

    /**
     * 공통으로 요청을 보내고 응답을 처리하는 메서드
     * @param url 요청할 URL
     * @param requestBody 요청 바디 객체
     * @param responseType 응답 타입 클래스
     * @return 응답 객체
     */
    private fun <T> sendRequest(
        url: String,
        requestBody: EditRequestToPython,
        responseType: Class<T>,
    ): T =
        try {
            val responseEntity = requestToPythonServerTemplate.postForEntity(url, requestBody, responseType)
            responseEntity.body ?: throw SurveyAIProcessingFailedException()
        } catch (exception: HttpClientErrorException) {
            throw PythonServerExceptionMapper.mapException(exception)
        } catch (exception: Exception) {
            throw SurveyAIProcessingFailedException()
        }
}
