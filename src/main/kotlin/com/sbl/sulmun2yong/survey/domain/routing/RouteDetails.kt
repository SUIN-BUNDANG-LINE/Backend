package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.SectionResponse
import java.util.UUID

interface RouteDetails {
    val type: SectionRouteType
    val nextSectionId: UUID?
    val keyQuestionId: UUID?
    val sectionRouteConfigs: SectionRouteConfigs?

    fun getDestinationSectionIdSet(): Set<UUID?>

    fun findNextSectionId(sectionResponse: SectionResponse): UUID?
}
