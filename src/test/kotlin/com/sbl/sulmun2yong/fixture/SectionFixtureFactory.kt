package com.sbl.sulmun2yong.fixture

import com.sbl.sulmun2yong.survey.domain.Section
import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
import java.util.UUID

object SectionFixtureFactory {
    const val TITLE = "섹션 제목"
    const val DESCRIPTION = "섹션 설명"

    fun createSection(
        id: UUID = UUID.randomUUID(),
        title: String = TITLE,
        description: String = DESCRIPTION,
        routeDetails: RouteDetails,
        questions: List<Question>,
    ) = Section(
        id = id,
        title = title + id,
        description = description + id,
        routeDetails = routeDetails,
        questions = questions,
    )
}
