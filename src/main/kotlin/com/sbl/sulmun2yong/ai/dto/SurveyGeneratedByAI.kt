package com.sbl.sulmun2yong.ai.dto

class SurveyGeneratedByAI(
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: List<SectionGeneratedByAI>,
)
