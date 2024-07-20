package com.sbl.sulmun2yong.fixture

import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfig
import com.sbl.sulmun2yong.survey.domain.routing.SectionRouteConfigs
import com.sbl.sulmun2yong.survey.domain.routing.SetByChoiceRouting
import java.util.UUID

object RoutingFixtureFactory {
    fun createSetByChoiceRouting(
        keyQuestionId: UUID = UUID.randomUUID(),
        contentIdMap: Map<String?, UUID?> = mapOf("a" to UUID.randomUUID(), "b" to UUID.randomUUID()),
    ) = SetByChoiceRouting(keyQuestionId = keyQuestionId, sectionRouteConfigs = createSectionRouteConfigs(contentIdMap))

    fun createSectionRouteConfigs(contentIdMap: Map<String?, UUID?>) =
        SectionRouteConfigs(contentIdMap.map { (content, id) -> SectionRouteConfig(content, id) })
}
