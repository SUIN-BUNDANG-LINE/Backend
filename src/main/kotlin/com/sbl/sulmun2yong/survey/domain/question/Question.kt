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

    fun isValidResponse(questionResponse: QuestionResponse): Boolean
}
