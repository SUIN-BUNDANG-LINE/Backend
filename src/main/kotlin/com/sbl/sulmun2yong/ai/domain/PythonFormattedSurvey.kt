package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.UUID

class PythonFormattedSurvey(
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: List<PythonFormattedSection>,
) {
    fun toNewSurvey(): Survey {
        val sectionIds = List(sections.size) { SectionId.Standard(UUID.randomUUID()) }
        val sectionIdsManger = SectionIds.from(sectionIds)

        val sections =
            sections.mapIndexed { index, sectionGeneratedByAI ->
                sectionGeneratedByAI.toSection(
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

    fun toUpdatedSurvey(survey: Survey): Survey {
        val sectionIds = List(sections.size) { SectionId.Standard(UUID.randomUUID()) }
        val sectionIdsManger = SectionIds.from(sectionIds)

        val sections =
            sections.mapIndexed { index, sectionGeneratedByAI ->
                sectionGeneratedByAI.toSection(
                    sectionIds[index],
                    sectionIdsManger,
                )
            }

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

    companion object {
        fun of(survey: Survey) =
            PythonFormattedSurvey(
                title = survey.title,
                description = survey.description,
                finishMessage = survey.finishMessage,
                sections = survey.sections.map { PythonFormattedSection.of(it) },
            )
    }
}
