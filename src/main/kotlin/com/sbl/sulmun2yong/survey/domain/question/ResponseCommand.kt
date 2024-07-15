package com.sbl.sulmun2yong.survey.domain.question

import com.sbl.sulmun2yong.survey.exception.InvalidResponseCommandException

data class ResponseCommand(
    val responseDetails: List<ResponseDetail>,
) {
    init {
        // 응답은 비어있으면 안된다
        if (responseDetails.isEmpty()) throw InvalidResponseCommandException()
        // 응답의 내용은 중복되면 안된다
        if (responseDetails.size != responseDetails.distinctBy { it }.size) throw InvalidResponseCommandException()
        // 기타 응답은 하나보다 많이 올 수 없다
        if (responseDetails.count { it.isEtc } > 1) throw InvalidResponseCommandException()
    }
}

data class ResponseDetail(
    val content: String,
    val isEtc: Boolean = false,
)
