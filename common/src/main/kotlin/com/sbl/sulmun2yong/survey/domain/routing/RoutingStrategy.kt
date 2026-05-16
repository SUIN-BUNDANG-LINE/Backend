package com.sbl.sulmun2yong.survey.domain.routing

import com.sbl.sulmun2yong.survey.domain.question.choice.Choice
import com.sbl.sulmun2yong.survey.domain.response.ResponseDetail
import com.sbl.sulmun2yong.survey.domain.response.SectionResponse
import com.sbl.sulmun2yong.survey.domain.section.SectionId
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import java.util.UUID

/** 다음 섹션으로 이동할 때의 전략(사용자 지정, 선택지 기반, 번호순) */
sealed class RoutingStrategy(
    val type: RoutingType,
) {
    /** 해당 라우팅 전략으로 이동할 수 있는 다음 섹션 ID의 목록 */
    abstract fun getNextSectionIds(): List<SectionId>

    /** (사용자 설정 라우팅) 유저가 선택한 섹션을 다음 섹션으로 결정 */
    data class SetByUser(
        val nextSectionId: SectionId,
    ) : RoutingStrategy(RoutingType.SET_BY_USER) {
        override fun getNextSectionIds() = listOf(nextSectionId)
    }

    /** (선택지 기반 라우팅) keyQuestion에 대해 응답한 선택지에 따라 다음 섹션으로 이동 */
    data class SetByChoice(
        val keyQuestionId: UUID,
        /** 선택지에 따른 다음 섹션 ID */
        val routingMap: Map<Choice, SectionId>,
    ) : RoutingStrategy(RoutingType.SET_BY_CHOICE) {
        override fun getNextSectionIds() = routingMap.values.toList()

        /** keyQuestion에 대한 응답에 따라 다음 섹션 ID를 찾아 반환 */
        fun findNextSectionId(sectionResponse: SectionResponse): SectionId {
            // 응답에서 keyQuestion에 해당하는 응답을 찾는다.
            val keyResponseDetail = sectionResponse.findKeyResponse() ?: throw InvalidSectionResponseException()
            // 찾은 응답에 따라 nextSectionId를 반환
            return routingMap[keyResponseDetail.toChoice()] ?: throw InvalidSectionResponseException()
        }

        fun getChoiceSet() = routingMap.keys

        private fun SectionResponse.findKeyResponse() = this.find { it.questionId == keyQuestionId && it.size == 1 }?.first()

        private fun ResponseDetail.toChoice() = if (isOther) Choice.Other else Choice.Standard(content)
    }

    /** (번호순 라우팅) 다음 번호에 해당하는 섹션으로 이동 */
    data object NumericalOrder : RoutingStrategy(RoutingType.NUMERICAL_ORDER) {
        override fun getNextSectionIds() = emptyList<SectionId>()
    }
}
