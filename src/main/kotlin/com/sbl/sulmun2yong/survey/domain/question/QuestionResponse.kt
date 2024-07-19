package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidQuestionResponseException
import java.util.UUID

data class QuestionResponse(
    val questionId: UUID,
    private val responses: List<ResponseDetail>,
) : List<ResponseDetail> by responses {
    init {
        if (responses.isEmpty()) throw InvalidQuestionResponseException()
        if (responses.hasDuplicates()) throw InvalidQuestionResponseException()
        if (responses.hasInvalidEtcCount()) throw InvalidQuestionResponseException()
    }

    private fun List<ResponseDetail>.hasDuplicates(): Boolean = this.size != this.distinctBy { it }.size

    private fun List<ResponseDetail>.hasInvalidEtcCount(): Boolean = this.count { it.isOther } > 1
}
