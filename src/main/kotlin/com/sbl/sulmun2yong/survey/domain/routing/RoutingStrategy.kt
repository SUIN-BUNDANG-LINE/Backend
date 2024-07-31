package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import java.util.UUID

sealed class RoutingStrategy(
    val type: RoutingType,
) {
    abstract fun getNextSectionIds(): List<SectionId>

    data class SetByUser(
        val nextSectionId: SectionId,
    ) : RoutingStrategy(RoutingType.SET_BY_USER) {
        override fun getNextSectionIds() = listOf(nextSectionId)
    }

    data class SetByChoice(
        val keyQuestionId: UUID,
        val routingMap: Map<Choice, SectionId>,
    ) : RoutingStrategy(RoutingType.SET_BY_CHOICE) {
        override fun getNextSectionIds() = routingMap.values.toList()

        fun findNextSectionId(sectionResponse: SectionResponse): SectionId {
            val keyResponseDetail = sectionResponse.findKeyResponse() ?: throw InvalidSectionResponseException()
            return routingMap[keyResponseDetail.toChoice()] ?: throw InvalidSectionResponseException()
        }

        fun getChoiceSet() = routingMap.keys

        private fun SectionResponse.findKeyResponse() = this.find { it.questionId == keyQuestionId && it.size == 1 }?.first()

        private fun ResponseDetail.toChoice() = if (isOther) Choice.Other else Choice.Standard(content)
    }

    data object NumericalOrder : RoutingStrategy(RoutingType.NUMERICAL_ORDER) {
        override fun getNextSectionIds() = emptyList<SectionId>()
    }
}
