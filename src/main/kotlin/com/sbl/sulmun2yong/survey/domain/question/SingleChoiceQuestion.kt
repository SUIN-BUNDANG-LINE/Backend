package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidQuestionException
import java.util.UUID

data class SingleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: List<String>,
    override val isAllowOther: Boolean,
) : Question {
    override val questionType: QuestionType = QuestionType.SINGLE_CHOICE

    init {
        if (choices.isEmpty()) throw InvalidQuestionException()
        if (choices.size != choices.distinct().size) throw InvalidQuestionException()
    }

    override fun isValidResponse(responseCommand: ResponseCommand): Boolean {
        if (responseCommand.responses.size != 1) return false
        if (responseCommand.responses.first().isOther) return isAllowOther
        return choices.contains(responseCommand.responses.first().content)
    }
}
