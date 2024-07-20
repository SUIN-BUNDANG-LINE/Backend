package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import com.sbl.sulmun2yong.survey.exception.InvalidRouteDetailsException
import java.util.UUID

sealed class RouteDetails(val type: SectionRouteType) {
    abstract fun getDestinationSectionIdSet(): Set<UUID?>

    data class NumericalOrder(val nextSectionId: UUID?) : RouteDetails(SectionRouteType.NUMERICAL_ORDER) {
        override fun getDestinationSectionIdSet() = setOf(nextSectionId)
    }

    data class SetByChoice(
        val keyQuestionId: UUID,
        val sectionRouteConfigs: List<SectionRouteConfig>,
    ) : RouteDetails(SectionRouteType.SET_BY_CHOICE) {
        init {
            if (sectionRouteConfigs.size != getContentsSet().size) throw InvalidRouteDetailsException()
        }

        override fun getDestinationSectionIdSet() = sectionRouteConfigs.map { it.nextSectionId }.toSet()

        fun findNextSectionId(responseDetail: ResponseDetail): UUID? {
            val content = if (responseDetail.isOther) null else responseDetail.content
            val routeConfig =
                sectionRouteConfigs.find { it.content == content }
                    ?: throw InvalidRouteDetailsException()
            return routeConfig.nextSectionId
        }

        fun getContentsSet() = sectionRouteConfigs.map { it.content }.toSet()
    }

    data class SetByUser(val nextSectionId: UUID?) : RouteDetails(SectionRouteType.SET_BY_USER) {
        override fun getDestinationSectionIdSet() = setOf(nextSectionId)
    }
}

data class SectionRouteConfig(
    val content: String?, // null이면 기타 응답
    val nextSectionId: UUID?, // null이면 설문 종료
)
