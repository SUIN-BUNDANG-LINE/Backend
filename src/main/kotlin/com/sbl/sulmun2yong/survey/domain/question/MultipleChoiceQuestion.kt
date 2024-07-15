package com.sbl.sulmun2yong.survey.domain.question

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

    override fun isValidResponse(responseCommand: ResponseCommand): Boolean {
        // TODO: 유효성 검사 로직 구현하기
        return true
    }
}
