package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidRouteDetailsException
import java.util.UUID

sealed class RouteDetails(val type: SectionRouteType) {
    data class NumericalOrder(val nextSectionId: UUID?) : RouteDetails(SectionRouteType.NUMERICAL_ORDER)

    data class SetByChoice(
        val keyQuestionId: UUID,
        val sectionRouteConfigs: List<SectionRouteConfig>,
    ) : RouteDetails(SectionRouteType.SET_BY_CHOICE) {
        init {
            if (sectionRouteConfigs.size != getContentsSet().size) throw InvalidRouteDetailsException()
        }

        fun findNextSectionId(responseDetail: ResponseDetail): UUID? {
            val content = if (responseDetail.isOther) null else responseDetail.content
            val routeConfig =
                sectionRouteConfigs.find { it.content == content }
                    ?: throw InvalidRouteDetailsException()
            return routeConfig.nextSectionId
        }

        private fun getContentsSet() = sectionRouteConfigs.map { it.content }.toSet()

        fun isValidSectionRouteConfig(
            isAllowOther: Boolean,
            choices: List<String>,
        ): Boolean {
            val choicesSet = if (isAllowOther) choices.toSet() + null else choices.toSet()
            return getContentsSet() == choicesSet
        }
    }

    data class SetByUser(val nextSectionId: UUID?) : RouteDetails(SectionRouteType.SET_BY_USER)
}

data class SectionRouteConfig(
    val content: String?, // null이면 기타 응답
    val nextSectionId: UUID?, // null이면 설문 종료
)
