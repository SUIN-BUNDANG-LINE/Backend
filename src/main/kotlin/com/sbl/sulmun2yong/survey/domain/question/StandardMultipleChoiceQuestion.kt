package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

/** 일반 다중 선택지 질문 */
data class StandardMultipleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: Choices,
) : MultipleChoiceQuestion {
    override val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE
}
