package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

data class MultipleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: Choices,
) : Question {
    override val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE

    override fun isValidResponse(questionResponse: QuestionResponse) = questionResponse.all { choices.isContains(it) }

    override fun canBeKeyQuestion() = false
}
