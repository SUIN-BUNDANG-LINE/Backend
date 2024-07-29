package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.NextSectionId
import com.sbl.sulmun2yong.survey.domain.SectionResponse
import com.sbl.sulmun2yong.survey.domain.question.Choice
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import java.util.UUID

sealed class RouteDetails(
    val type: SectionRouteType,
) {
    abstract fun isSectionIdsValid(sectionIds: List<UUID>): Boolean

    data class SetByUserRouting(
        val nextSectionId: NextSectionId,
    ) : RouteDetails(SectionRouteType.SET_BY_USER) {
        override fun isSectionIdsValid(sectionIds: List<UUID>) = if (nextSectionId.isEnd) true else sectionIds.contains(nextSectionId.value)
    }

    data class SetByChoiceRouting(
        val keyQuestionId: UUID,
        val sectionRouteConfigs: Map<Choice, NextSectionId>,
    ) : RouteDetails(SectionRouteType.SET_BY_CHOICE) {
        override fun isSectionIdsValid(sectionIds: List<UUID>) = sectionIds.containsAll(sectionRouteConfigs.values.mapNotNull { it.value })

        fun findNextSectionId(sectionResponse: SectionResponse): NextSectionId {
            val keyResponseDetail = sectionResponse.findKeyResponse() ?: throw InvalidSectionResponseException()
            return sectionRouteConfigs[keyResponseDetail.toChoice()] ?: throw InvalidSectionResponseException()
        }

        fun getChoiceSet() = sectionRouteConfigs.keys

        private fun SectionResponse.findKeyResponse() = this.find { it.questionId == keyQuestionId && it.size == 1 }?.first()

        private fun ResponseDetail.toChoice() = if (isOther) Choice.Other else Choice.Standard(content)
    }

    data object NumericalOrderRouting : RouteDetails(SectionRouteType.NUMERICAL_ORDER) {
        override fun isSectionIdsValid(sectionIds: List<UUID>) = true
    }
}
