package com.sbl.sulmun2yong.survey.domain.question

import java.util.UUID

/** 일반 주관식 질문 */
data class StandardTextResponseQuestion(
    override val id: UUID,
    override val title: String,
    override val description: String,
    override val isRequired: Boolean,
) : TextResponseQuestion {
    override val questionType: QuestionType = QuestionType.TEXT_RESPONSE
    override val choices = null
}
