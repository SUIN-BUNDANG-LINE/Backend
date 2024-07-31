package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
import java.util.UUID

/** 일반 단일 선택지 질문 */
data class StandardSingleChoiceQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
    override val choices: Choices,
) : SingleChoiceQuestion {
    override val questionType: QuestionType = QuestionType.SINGLE_CHOICE
}
