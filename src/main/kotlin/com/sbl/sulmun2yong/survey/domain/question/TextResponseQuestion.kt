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

    override fun isValidResponse(responseCommand: ResponseCommand): Boolean {
        // TODO: 유효성 검사 로직 구현하기
        return true
    }
}
