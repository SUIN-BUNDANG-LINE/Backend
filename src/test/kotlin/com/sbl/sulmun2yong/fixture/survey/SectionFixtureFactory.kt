package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
import com.sbl.sulmun2yong.survey.domain.section.Section
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.util.UUID

object SectionFixtureFactory {
    const val TITLE = "섹션 제목"
    const val DESCRIPTION = "섹션 설명"

    fun createSection(
        id: UUID = UUID.randomUUID(),
        title: String = TITLE,
        description: String = DESCRIPTION,
        routeDetails: RouteDetails = RouteDetails.NumericalOrderRouting,
        questions: List<Question>,
        sectionIds: List<UUID> = listOf(id),
    ) = Section(
        id = SectionId.Standard(id),
        title = title + id,
        description = description + id,
        routeDetails = routeDetails,
        questions = questions,
        sectionIds = SectionIds.from(sectionIds.map { SectionId.Standard(it) }),
    )

    fun createMockSection(
        id: UUID,
        nextSectionId: SectionId,
        sectionIds: List<UUID>,
    ): Section {
        val section = mock<Section>()
        `when`(section.id).thenReturn(SectionId.Standard(id))
        `when`(section.findNextSectionId(any())).thenReturn(nextSectionId)
        `when`(section.sectionIds).thenReturn(SectionIds.from(sectionIds.map { SectionId.Standard(it) }))
        return section
    }

    fun createSectionIds(sectionIds: List<UUID>) = SectionIds.from(sectionIds.map { SectionId.Standard(it) })
}
