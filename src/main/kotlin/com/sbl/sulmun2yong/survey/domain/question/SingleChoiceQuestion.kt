package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

data class SingleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: Choices,
) : Question {
    override val questionType: QuestionType = QuestionType.SINGLE_CHOICE

    override fun isValidResponse(questionResponse: QuestionResponse): Boolean {
        if (questionResponse.size != 1) return false
        return choices.isContains(questionResponse.first())
    }

    override fun canBeKeyQuestion() = isRequired

    fun getChoiceSet() = choices.getChoiceSet()
}
