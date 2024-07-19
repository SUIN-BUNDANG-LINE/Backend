package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

data class SingleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: Choices,
    override val isAllowOther: Boolean,
) : Question {
    override val questionType: QuestionType = QuestionType.SINGLE_CHOICE

    override fun isValidResponse(questionResponse: QuestionResponse): Boolean {
        if (questionResponse.size != 1) return false
        if (questionResponse.first().isOther) return isAllowOther
        return choices.contains(questionResponse.first().content)
    }
}
