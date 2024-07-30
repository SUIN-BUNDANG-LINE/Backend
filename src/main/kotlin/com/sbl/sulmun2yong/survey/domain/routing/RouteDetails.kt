package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.question.Choice
import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.domain.section.SectionIds
import com.sbl.sulmun2yong.survey.domain.section.SectionResponse
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import java.util.UUID

sealed class RouteDetails(
    val type: SectionRouteType,
) {
    abstract fun isSectionIdsValid(sectionIds: SectionIds): Boolean

    data class SetByUserRouting(
        val nextSectionId: SectionId,
    ) : RouteDetails(SectionRouteType.SET_BY_USER) {
        override fun isSectionIdsValid(sectionIds: SectionIds) = sectionIds.isContains(nextSectionId)
    }

    data class SetByChoiceRouting(
        val keyQuestionId: UUID,
        val sectionRouteConfigs: Map<Choice, SectionId>,
    ) : RouteDetails(SectionRouteType.SET_BY_CHOICE) {
        override fun isSectionIdsValid(sectionIds: SectionIds) = sectionRouteConfigs.values.all { sectionIds.isContains(it) }

        fun findNextSectionId(sectionResponse: SectionResponse): SectionId {
            val keyResponseDetail = sectionResponse.findKeyResponse() ?: throw InvalidSectionResponseException()
            return sectionRouteConfigs[keyResponseDetail.toChoice()] ?: throw InvalidSectionResponseException()
        }

        fun getChoiceSet() = sectionRouteConfigs.keys

        private fun SectionResponse.findKeyResponse() = this.find { it.questionId == keyQuestionId && it.size == 1 }?.first()

        private fun ResponseDetail.toChoice() = if (isOther) Choice.Other else Choice.Standard(content)
    }

    data object NumericalOrderRouting : RouteDetails(SectionRouteType.NUMERICAL_ORDER) {
        override fun isSectionIdsValid(sectionIds: SectionIds) = true
    }
}
