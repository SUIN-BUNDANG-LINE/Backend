package com.sbl.sulmun2yong.survey.domain.question.impl

import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.TextQuestion
import java.util.UUID

/** 일반 주관식 질문 */
data class StandardTextQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
) : TextQuestion {
    override val questionType: QuestionType = QuestionType.TEXT_RESPONSE
    override val choices = null
}
