package com.sbl.sulmun2yong.ai.domain

class SurveyGeneratedByAI(
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: List<SectionGeneratedByAI>,
)
