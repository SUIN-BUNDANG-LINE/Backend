package com.sbl.sulmun2yong.ai.domain

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.UUID

class PythonFormattedSection(
    val id: UUID? = null,
    val title: String,
    val description: String,
    val questions: List<PythonFormattedQuestion>,
) {
    fun toSection(
        sectionId: SectionId.Standard,
        sectionIds: SectionIds,
    ): Section {
        val defaultRoutingStrategy = RoutingStrategy.NumericalOrder

        return Section(
            id = sectionId,
            title = title,
            description = description,
            routingStrategy = defaultRoutingStrategy,
            questions = questions.map { it.toQuestion() },
            sectionIds = sectionIds,
        )
    }

    fun toUpdatedSurvey(
        sectionId: UUID,
        survey: Survey,
    ): Survey {
        val updatedSections =
            survey.sections.map { section ->
                if (section.id.value == sectionId) {
                    this.toSection(section.id, section.sectionIds)
                } else {
                    section
                }
            }

        return survey.updateContent(
            title = survey.title,
            description = survey.description,
            thumbnail = survey.thumbnail,
            finishMessage = survey.finishMessage,
            rewardSetting = survey.rewardSetting,
            isVisible = survey.isVisible,
            sections = updatedSections,
        )
    }

    companion object {
        fun from(section: Section) =
            PythonFormattedSection(
                id = section.id.value,
                title = section.title,
                description = section.description,
                questions = section.questions.map { PythonFormattedQuestion.from(it) },
            )
    }
}
