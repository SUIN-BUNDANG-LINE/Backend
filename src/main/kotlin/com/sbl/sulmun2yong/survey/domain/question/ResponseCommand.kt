package com.sbl.sulmun2yong.survey.domain.question

data class ResponseCommand(
    val responseDetails: List<ResponseDetail>,
)

data class ResponseDetail(
    val content: String,
    val isEtc: Boolean = false,
)
