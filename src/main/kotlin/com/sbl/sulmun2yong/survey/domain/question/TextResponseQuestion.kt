package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

data class TextResponseQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
) : Question {
    override val questionType: QuestionType = QuestionType.TEXT_RESPONSE
    override val choices = null
    override val isAllowOther = false

    override fun isValidResponse(responseCommand: ResponseCommand) =
        responseCommand.responses.size == 1 && !responseCommand.responses.first().isOther
}
