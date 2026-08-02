package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import java.util.UUID

object SectionFixtureFactory {
    const val TITLE = "섹션 제목"
    const val DESCRIPTION = "섹션 설명"

    fun createSection(
        id: UUID = UUID.randomUUID(),
        title: String = TITLE,
        description: String = DESCRIPTION,
        routingStrategy: RoutingStrategy = RoutingStrategy.NumericalOrder,
        questions: List<Question>,
        sectionIds: List<UUID> = listOf(id),
    ) = Section(
        id = SectionId.Standard(id),
        title = title + id,
        description = description + id,
        routingStrategy = routingStrategy,
        questions = questions,
        sectionIds = SectionIds.from(sectionIds.map { SectionId.Standard(it) }),
    )

    @Suppress("UNUSED_PARAMETER")
    fun createMockSection(
        id: UUID,
        nextSectionId: SectionId,
        sectionIds: List<UUID>,
    ): Section =
        Section(
            id = SectionId.Standard(id),
            title = "",
            description = "",
            routingStrategy = RoutingStrategy.NumericalOrder,
            questions = emptyList(),
            sectionIds = SectionIds.from(sectionIds.map { SectionId.Standard(it) }),
        )

    fun createSectionIds(sectionIds: List<UUID>) = SectionIds.from(sectionIds.map { SectionId.Standard(it) })
}
