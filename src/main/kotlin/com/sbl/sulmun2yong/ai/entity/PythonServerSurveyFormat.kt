package com.sbl.sulmun2yong.ai.entity

import com.sbl.sulmun2yong.survey.domain.Survey
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.UUID

class PythonServerSurveyFormat(
    val title: String,
    val description: String,
    val finishMessage: String,
    val sections: List<PythonServerSectionFormat>,
) {
    fun toNewSurvey(): Survey {
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

    companion object {
        fun of(survey: Survey) =
            PythonServerSurveyFormat(
                title = survey.title,
                description = survey.description,
                finishMessage = survey.finishMessage,
                sections = survey.sections.map { PythonServerSectionFormat.of(it) },
            )
    }
}