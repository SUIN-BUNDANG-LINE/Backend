package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.SectionResponse
import java.util.UUID

data class SetByUserRouting(
    override val nextSectionId: UUID?,
) : RouteDetails {
    override val type: SectionRouteType = SectionRouteType.SET_BY_USER
    override val keyQuestionId = null
    override val sectionRouteConfigs = null

    override fun getDestinationSectionIdSet(): Set<UUID?> = setOf(nextSectionId)

    override fun findNextSectionId(sectionResponse: SectionResponse) = nextSectionId
}
