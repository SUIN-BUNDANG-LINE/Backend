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

    override fun validateQuestionResponse(questionResponse: QuestionResponse): Boolean {
        for (responseDetail in questionResponse) {
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
