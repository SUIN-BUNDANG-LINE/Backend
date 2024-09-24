package com.sbl.sulmun2yong.ai.dto

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.UUID

class SurveyGeneratedByAI(
    private val title: String,
    private val description: String,
    private val finishMessage: String,
    private val sections: List<SectionGeneratedByAI>,
) {
    fun toDomain(): Survey {
        val sectionIds = List(sections.size) { SectionId.Standard(UUID.randomUUID()) }
        val sectionIdsManger = SectionIds.from(sectionIds)

        val sections =
            sections.mapIndexed { index, sectionGeneratedByAI ->
                sectionGeneratedByAI.toDomain(
                    sectionIds[index],
                    sectionIdsManger,
                )
            }

        val survey = Survey.create(UUID.randomUUID())
        return survey.updateContent(
            title = title,
            description = description,
            thumbnail = survey.thumbnail,
            finishMessage = finishMessage,
            rewardSetting = survey.rewardSetting,
            isVisible = false,
            sections = sections,
        )
    }
}
