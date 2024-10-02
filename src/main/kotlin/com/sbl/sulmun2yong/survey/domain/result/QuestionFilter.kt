package com.sbl.sulmun2yong.survey.domain.result

import com.sbl.sulmun2yong.survey.exception.InvalidQuestionFilterException
import java.util.UUID

data class QuestionFilter(
    val questionId: UUID,
    val contents: List<String>,
    val isPositive: Boolean,
) {
    init {
        require(contents.isNotEmpty()) { throw InvalidQuestionFilterException() }
    }
}
