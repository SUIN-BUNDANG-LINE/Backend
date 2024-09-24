package com.sbl.sulmun2yong.survey.domain.result

import com.sbl.sulmun2yong.survey.exception.InvalidResultFilterException

data class ResultFilter(
    val questionFilters: List<QuestionFilter>,
) {
    companion object {
        const val MAX_SIZE = 20
    }

    init {
        require(questionFilters.size <= MAX_SIZE) { throw InvalidResultFilterException() }
    }
}
