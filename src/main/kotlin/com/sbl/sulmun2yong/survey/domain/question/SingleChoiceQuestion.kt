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

    override fun isValidResponse(responseCommand: ResponseCommand): Boolean {
        if (responseCommand.responses.size != 1) return false
        if (responseCommand.responses.first().isOther) return isAllowOther
        return choices.contains(responseCommand.responses.first().content)
    }
}
