package com.sbl.sulmun2yong.ai.dto

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import java.util.UUID

class QuestionGeneratedByAI(
    val questionType: QuestionType,
    val title: String,
    val isRequired: Boolean,
    val choices: List<String>,
    val isAllowOther: Boolean,
) {
    fun toDomain() =
        Question(
            id = UUID.randomUUID(),
            title = title,
            questionType = questionType,
            isRequired = isRequired,
            choices = choices,
            isAllowOther = isAllowOther,
        )
}
