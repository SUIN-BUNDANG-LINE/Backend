package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.SectionResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import java.util.UUID

data class SetByChoiceRouting(
    override val keyQuestionId: UUID,
    override val sectionRouteConfigs: SectionRouteConfigs,
) : RouteDetails {
    override val type: SectionRouteType = SectionRouteType.SET_BY_CHOICE
    override val nextSectionId = null

    override fun getDestinationSectionIdSet() = sectionRouteConfigs.map { it.nextSectionId }.toSet()

    override fun findNextSectionId(sectionResponse: SectionResponse): UUID? {
        val keyResponseDetail = sectionResponse.findKeyQuestionResponseDetail() ?: throw InvalidSectionResponseException()
        val content = if (keyResponseDetail.isOther) null else keyResponseDetail.content
        val routeConfig = sectionRouteConfigs.findByContent(content) ?: throw InvalidSectionResponseException()
        return routeConfig.nextSectionId
    }

    fun getContentSet() = sectionRouteConfigs.map { it.content }.toSet()

    private fun SectionResponse.findKeyQuestionResponseDetail() = this.find { it.questionId == keyQuestionId && it.size == 1 }?.first()
}
