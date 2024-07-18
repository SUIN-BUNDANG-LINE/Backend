package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

data class MultipleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: Choices,
    override val isAllowOther: Boolean,
) : Question {
    override val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE

    override fun isValidResponse(responseCommand: ResponseCommand): Boolean {
        for (responseDetail in responseCommand.responses) {
            println(responseDetail)
            if (responseDetail.isOther) {
                if (isAllowOther) continue
                return false
            } else if (!choices.contains(responseDetail.content)) {
                return false
            }
        }
        return true
    }
}
