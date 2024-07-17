package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.ResponseDetail
import java.util.UUID

sealed class RouteDetails(val type: SectionRouteType) {
    data class NumericalOrder(val nextSectionId: UUID?) : RouteDetails(SectionRouteType.NUMERICAL_ORDER)

    data class SetByChoice(
        val keyQuestionId: UUID,
        val sectionRouteConfigs: List<SectionRouteConfig>,
    ) : RouteDetails(SectionRouteType.SET_BY_CHOICE) {
        fun findNextSectionId(responseDetail: ResponseDetail): UUID? {
            // TODO responseDetail을 이용하여 SectionRouteConfig에서 nextSectionId를 찾아서 반환
            return null
        }
    }

    data class SetByUser(val nextSectionId: UUID?) : RouteDetails(SectionRouteType.SET_BY_USER)
}

data class SectionRouteConfig(
    val content: String?, // null이면 기타 응답
    val nextSectionId: UUID?, // null이면 설문 종료
)
