package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.exception.InvalidSectionException
import java.util.UUID

data class Section(
    val id: UUID,
    val title: String,
    val description: String,
    val routeDetails: RouteDetails,
    val questions: List<Question>,
) {
    init {
        if (routeDetails is RouteDetails.SetByChoice) {
            val keyQuestion =
                questions.find { it.id == routeDetails.keyQuestionId }
                    ?: throw InvalidSectionException()
            if (keyQuestion.questionType != QuestionType.SINGLE_CHOICE) throw InvalidSectionException()
            if (!keyQuestion.isRequired) throw InvalidSectionException()
            val isValidSectionConfigs =
                routeDetails.isValidSectionRouteConfig(
                    keyQuestion.isAllowOther,
                    (keyQuestion as SingleChoiceQuestion).choices,
                )
            if (isValidSectionConfigs.not()) throw InvalidSectionException()
        }
    }

    fun findNextSectionId(sectionResponses: List<SectionResponse>): UUID? {
        // TODO: sectionResponses 유효한지 확인
        // TODO: sectionResponses 이용하여 routeDetails에서 nextSectionId를 찾아서 반환
        return null
    }
}
