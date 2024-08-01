package com.sbl.sulmun2yong.survey.domain.response

import com.sbl.sulmun2yong.survey.exception.InvalidQuestionResponseException
import java.util.UUID

data class QuestionResponse(
    val questionId: UUID,
    private val responses: List<ResponseDetail>,
) : List<ResponseDetail> by responses {
    init {
        require(responses.isNotEmpty()) { throw InvalidQuestionResponseException() }
        require(isUnique()) { throw InvalidQuestionResponseException() }
        require(isEtcNotMoreThenOne()) { throw InvalidQuestionResponseException() }
    }

    private fun isUnique() = this.size == this.toSet().size

    private fun isEtcNotMoreThenOne() = this.count { it.isOther } <= 1
}
