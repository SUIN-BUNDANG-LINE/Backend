package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

interface Question {
    val id: UUID
    val questionType: QuestionType
    val title: String
    val description: String
    val isRequired: Boolean
    val choices: Choices?
    val isAllowOther: Boolean

    fun isValidResponse(questionResponse: QuestionResponse): Boolean {
        if (id != questionResponse.questionId) return false
        return validateQuestionResponse(questionResponse)
    }

    fun validateQuestionResponse(questionResponse: QuestionResponse): Boolean

    fun canBeKeyQuestion() = questionType == QuestionType.SINGLE_CHOICE && isRequired
}
