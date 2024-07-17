package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidResponseCommandException

data class ResponseCommand(
    val responses: List<ResponseDetail>,
) {
    init {
        if (responses.isEmpty()) throw InvalidResponseCommandException()
        if (responses.hasDuplicates()) throw InvalidResponseCommandException()
        if (responses.hasInvalidEtcCount()) throw InvalidResponseCommandException()
    }

    private fun List<ResponseDetail>.hasDuplicates(): Boolean = this.size != this.distinctBy { it }.size

    private fun List<ResponseDetail>.hasInvalidEtcCount(): Boolean = this.count { it.isOther } > 1
}

data class ResponseDetail(
    val content: String,
    val isOther: Boolean = false,
)
