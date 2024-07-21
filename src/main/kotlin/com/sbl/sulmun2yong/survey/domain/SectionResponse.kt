package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import java.util.UUID

data class SectionResponse(
    val sectionId: UUID,
    private val questionResponses: List<QuestionResponse>,
) : List<QuestionResponse> by questionResponses {
    init {
        require(isQuestionResponsesUnique()) { throw InvalidSurveyResponseException() }
    }

    private fun isQuestionResponsesUnique() = questionResponses.map { it.questionId }.toSet().size == questionResponses.size
}
