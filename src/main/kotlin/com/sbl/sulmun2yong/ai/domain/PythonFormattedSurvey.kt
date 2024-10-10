package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.UUID

class PythonFormattedSurvey(
    val id: UUID? = null,
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: List<PythonFormattedSection>,
) {
    fun toNewSurvey(originalSurvey: Survey): Survey {
        val sectionIds = List(sections.size) { SectionId.Standard(UUID.randomUUID()) }
        val sectionIdsManger = SectionIds.from(sectionIds)

        val sections =
            sections.mapIndexed { index, sectionGeneratedByAI ->
                sectionGeneratedByAI.toSection(
                    sectionIds[index],
                    sectionIdsManger,
                )
            }

        return originalSurvey.updateContent(
            title = title,
            description = description,
            thumbnail = originalSurvey.thumbnail,
            finishMessage = finishMessage,
            rewardSetting = originalSurvey.rewardSetting,
            isVisible = originalSurvey.isVisible,
            sections = sections,
        )
    }

    fun toUpdatedSurvey(survey: Survey): Survey {
        if (sections.isEmpty()) {
            return survey.updateContent(
                title = title,
                description = description,
                thumbnail = survey.thumbnail,
                finishMessage = finishMessage,
                rewardSetting = survey.rewardSetting,
                isVisible = false,
                sections = listOf(),
            )
        }

        val sectionIds =
            sections.map { section ->
                section.id?.let {
                    SectionId.Standard(it)
                } ?: SectionId.Standard(UUID.randomUUID())
            }

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
        fun from(survey: Survey) =
            PythonFormattedSurvey(
                id = survey.id,
                title = survey.title,
                description = survey.description,
                finishMessage = survey.finishMessage,
                sections = survey.sections.map { PythonFormattedSection.from(it) },
            )
    }
}
