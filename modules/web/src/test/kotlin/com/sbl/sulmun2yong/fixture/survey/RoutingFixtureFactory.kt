package com.sbl.sulmun2yong.fixture.survey

import com.sbl.sulmun2yong.fixture.survey.SurveyConstFactory.CHOICE_SET
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

    /**
     * SetByChoice 라우팅 전략을 생성하고, mock 객체로 반환한다.
     * @param keyQuestionId 기본값 = UUID.randomUUID()
     * @param nextSectionId 기본값 = SectionId.End
     * @param choiceSet 기본값 = SurveyConstFactory.CHOICE_SET
     * @param sectionIds 기본값 = emptyList()
     * */
    fun createMockSetByChoiceRouting(
        keyQuestionId: UUID = UUID.randomUUID(),
        nextSectionId: SectionId = SectionId.End,
        choiceSet: Set<Choice> = CHOICE_SET,
        sectionIds: List<SectionId> = emptyList(),
    ): RoutingStrategy.SetByChoice {
        val mockSetByChoiceRouting = mock<RoutingStrategy.SetByChoice>()
        `when`(mockSetByChoiceRouting.keyQuestionId).thenReturn(keyQuestionId)
        `when`(mockSetByChoiceRouting.getNextSectionIds()).thenReturn(sectionIds)
        `when`(mockSetByChoiceRouting.findNextSectionId(any())).thenReturn(nextSectionId)
        `when`(mockSetByChoiceRouting.getChoiceSet()).thenReturn(choiceSet)
        return mockSetByChoiceRouting
    }
}
