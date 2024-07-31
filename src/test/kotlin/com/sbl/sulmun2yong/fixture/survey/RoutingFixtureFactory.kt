package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.routing.RoutingStrategy
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.util.UUID

object RoutingFixtureFactory {
    fun createSetByChoiceRouting(
        keyQuestionId: UUID = UUID.randomUUID(),
        contentIdMap: Map<String?, UUID?> = mapOf("a" to UUID.randomUUID(), "b" to UUID.randomUUID()),
    ) = RoutingStrategy.SetByChoice(
        keyQuestionId = keyQuestionId,
        routingMap = createContentIdMap(contentIdMap),
    )

    fun createContentIdMap(contentIdMap: Map<String?, UUID?>) =
        contentIdMap.map { (content, id) -> Choice.from(content) to SectionId.from(id) }.toMap()

    fun createMockSetByChoiceRouting(
        keyQuestionId: UUID = UUID.randomUUID(),
        nextSectionId: SectionId = SectionId.End,
        choiceSet: Set<Choice> =
            setOf(
                Choice.Standard("a"),
                Choice.Standard("b"),
                Choice.Standard("c"),
                Choice.Other,
            ),
        sectionIds: List<SectionId>,
    ): RoutingStrategy.SetByChoice {
        val mockSetByChoiceRouting = mock<RoutingStrategy.SetByChoice>()
        `when`(mockSetByChoiceRouting.keyQuestionId).thenReturn(keyQuestionId)
        `when`(mockSetByChoiceRouting.getNextSectionIds()).thenReturn(sectionIds)
        `when`(mockSetByChoiceRouting.findNextSectionId(any())).thenReturn(nextSectionId)
        `when`(mockSetByChoiceRouting.getChoiceSet()).thenReturn(choiceSet)
        return mockSetByChoiceRouting
    }
}
