package com.sbl.sulmun2yong.survey.domain.section

import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException

data class SectionResponse(
    val sectionId: SectionId.Standard,
    private val questionResponses: List<QuestionResponse>,
) : List<QuestionResponse> by questionResponses {
    init {
        require(isQuestionResponsesUnique()) { throw InvalidSurveyResponseException() }
    }

    private fun isQuestionResponsesUnique() = questionResponses.map { it.questionId }.toSet().size == questionResponses.size
}
