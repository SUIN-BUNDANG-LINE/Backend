package com.sbl.sulmun2yong.ai.dto

import com.sbl.sulmun2yong.survey.domain.Survey
import java.util.UUID

class SurveyGeneratedByAI(
    private val title: String,
    private val description: String,
    private val finishMessage: String,
    private val sections: List<SectionGeneratedByAI>,
) {
    fun toDomain(): Survey {
        val survey = Survey.create(UUID.randomUUID())
        survey.updateContent(
            title = title,
            description = description,
            thumbnail = survey.thumbnail,
            finishMessage = finishMessage,
            rewardSetting = survey.rewardSetting,
            isVisible = false,
            sections = sections.map { it.toDomain() },
        )
        return survey
    }
}
