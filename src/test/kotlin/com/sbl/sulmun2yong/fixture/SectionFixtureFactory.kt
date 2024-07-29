package com.sbl.sulmun2yong.fixture

import com.sbl.sulmun2yong.survey.domain.NextSectionId
import com.sbl.sulmun2yong.survey.domain.Section
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
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
        id = id,
        title = title + id,
        description = description + id,
        routeDetails = routeDetails,
        questions = questions,
        sectionIds = sectionIds,
    )

    fun createMockSection(
        id: UUID,
        nextSectionId: NextSectionId,
        sectionIds: List<UUID>,
    ): Section {
        val section = mock<Section>()
        `when`(section.id).thenReturn(id)
        `when`(section.findNextSectionId(any())).thenReturn(nextSectionId)
        `when`(section.sectionIds).thenReturn(sectionIds)
        return section
    }
}
