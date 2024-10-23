package com.sbl.sulmun2yong.ai.dto.python.response

import com.sbl.sulmun2yong.ai.domain.PythonFormattedQuestion
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import java.util.UUID

data class
QuestionResponseFromPython(
    val id: UUID?,
    val questionType: QuestionType,
    val title: String,
    val isRequired: Boolean,
    val choices: List<String>? = null,
    val isAllowOther: Boolean = false,
) {
    fun toDomain() =
        PythonFormattedQuestion(
            id = id,
            questionType = questionType,
            title = title,
            isRequired = isRequired,
            choices = choices,
            isAllowOther = isAllowOther,
        )
}
