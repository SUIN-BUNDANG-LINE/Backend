package com.sbl.sulmun2yong.fixture

import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfig
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfigs
import com.sbl.sulmun2yong.survey.domain.routing.SetByChoiceRouting
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.util.UUID

object RoutingFixtureFactory {
    fun createSetByChoiceRouting(
        keyQuestionId: UUID = UUID.randomUUID(),
        contentIdMap: Map<String?, UUID?> = mapOf("a" to UUID.randomUUID(), "b" to UUID.randomUUID()),
    ) = SetByChoiceRouting(keyQuestionId = keyQuestionId, sectionRouteConfigs = createSectionRouteConfigs(contentIdMap))

    fun createSectionRouteConfigs(contentIdMap: Map<String?, UUID?>) =
        SectionRouteConfigs(contentIdMap.map { (content, id) -> SectionRouteConfig(content, id) })

    fun createMockSetByChoiceRouting(
        keyQuestionId: UUID = UUID.randomUUID(),
        destinationSectionIdSet: Set<UUID?> = setOf(null),
        nextSectionId: UUID? = null,
        contentSet: Set<String?> = setOf("a", "b", "c", null),
    ): SetByChoiceRouting {
        val mockSetByChoiceRouting = mock<SetByChoiceRouting>()
        `when`(mockSetByChoiceRouting.keyQuestionId).thenReturn(keyQuestionId)
        `when`(mockSetByChoiceRouting.getDestinationSectionIdSet()).thenReturn(destinationSectionIdSet)
        `when`(mockSetByChoiceRouting.findNextSectionId(any())).thenReturn(nextSectionId)
        `when`(mockSetByChoiceRouting.getContentSet()).thenReturn(contentSet)
        return mockSetByChoiceRouting
    }
}
