package com.sbl.sulmun2yong.survey.domain.result

import com.sbl.sulmun2yong.survey.exception.InvalidResultDetailsException
import java.util.UUID

data class ResultDetails(
    val questionId: UUID,
    val participantId: UUID,
    val contents: List<String>,
) {
    init {
        require(contents.isNotEmpty()) { throw InvalidResultDetailsException() }
    }

    fun isMatched(questionFilter: QuestionFilter): Boolean {
        if (questionFilter.questionId != questionId) return false
        return contents.any { questionFilter.contents.contains(it) }
    }
}
