package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.Question
import java.util.UUID

data class Section(
    val id: UUID,
    val title: String,
    val description: String,
    val routeDetails: RouteDetails,
    val questions: List<Question>,
) {
    init {
        // TODO: RouteDetails와 섹션의 정보들이 유요한지 확인
    }

    fun findNextSectionId(sectionResponses: List<SectionResponse>): UUID? {
        // TODO: sectionResponses 유효한지 확인
        // TODO: sectionResponses 이용하여 routeDetails에서 nextSectionId를 찾아서 반환
        return null
    }
}
