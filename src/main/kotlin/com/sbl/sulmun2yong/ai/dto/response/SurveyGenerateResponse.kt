package com.sbl.sulmun2yong.ai.dto.response

data class SurveyGenerateResponse(
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: MutableList<Any>,
)
