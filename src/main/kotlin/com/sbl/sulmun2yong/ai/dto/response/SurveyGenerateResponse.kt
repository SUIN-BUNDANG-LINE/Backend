package com.sbl.sulmun2yong.ai.dto.response

data class SurveyGenerateResponse(
    private val title: String,
    private val description: String,
    private val finishMessage: String,
    private val sections: MutableList<Any>,
)
