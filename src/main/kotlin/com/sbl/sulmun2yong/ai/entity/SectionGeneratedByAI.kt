package com.sbl.sulmun2yong.ai.entity

import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds

class SectionGeneratedByAI(
    val title: String,
    val description: String,
    val questions: List<QuestionGeneratedByAI>,
) {
    fun toDomain(
        sectionId: SectionId.Standard,
        sectionIds: SectionIds,
    ): Section =
        Section(
            id = sectionId,
            title = title,
            description = description,
            routingStrategy = RoutingStrategy.NumericalOrder,
            questions = questions.map { it.toDomain() },
            sectionIds = sectionIds,
        )
}
