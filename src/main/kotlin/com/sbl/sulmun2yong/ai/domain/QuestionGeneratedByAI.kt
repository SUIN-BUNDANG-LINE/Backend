package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.question.QuestionType

class QuestionGeneratedByAI(
    val questionType: QuestionType,
    val title: String,
    val isRequired: Boolean,
    val choices: List<String>,
    val isAllowOther: Boolean,
)
