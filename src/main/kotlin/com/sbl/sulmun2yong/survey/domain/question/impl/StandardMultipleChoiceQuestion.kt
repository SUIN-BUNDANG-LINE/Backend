package com.sbl.sulmun2yong.survey.domain.question.impl

import com.sbl.sulmun2yong.survey.domain.question.MultipleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.choice.Choices
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
