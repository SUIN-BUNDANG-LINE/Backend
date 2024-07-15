package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidQuestionException
import java.util.UUID

data class MultipleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: List<String>,
    override val isAllowOther: Boolean,
) : Question {
    override val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE

    init {
        if (choices.isEmpty()) throw InvalidQuestionException()
    }

    override fun isValidResponse(responseCommand: ResponseCommand): Boolean {
        for (responseDetail in responseCommand.responseDetails) {
            println(responseDetail)
            if (responseDetail.isEtc) {
                if (isAllowOther) continue
                return false
            } else if (!choices.contains(responseDetail.content)) {
                return false
            }
        }
        return true
    }
}
