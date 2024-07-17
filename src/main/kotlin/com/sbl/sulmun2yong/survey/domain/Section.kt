package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.QuestionType
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.exception.InvalidSectionException
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
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

    companion object {
        fun create() =
            Section(
                id = UUID.randomUUID(),
                title = "",
                description = "",
                routeDetails = RouteDetails.NumericalOrder(null),
                questions = emptyList(),
            )
    }

    fun findNextSectionId(sectionResponses: List<SectionResponse>): UUID? {
        for (question in questions) {
            val findInResponse = sectionResponses.find { it.questionId == question.id }
            if (question.isRequired && findInResponse == null) {
                throw InvalidSectionResponseException()
            }
            if (findInResponse != null && !question.isValidResponse(findInResponse.questionResponses)) {
                throw InvalidSectionResponseException()
            }
        }

        return when (routeDetails) {
            is RouteDetails.SetByChoice -> {
                val sectionResponse =
                    sectionResponses.find { it.questionId == routeDetails.keyQuestionId }
                        ?: throw InvalidSectionResponseException()
                routeDetails.findNextSectionId(sectionResponse.questionResponses.responseDetails.first())
            }

            is RouteDetails.NumericalOrder -> {
                routeDetails.nextSectionId
            }

            is RouteDetails.SetByUser -> {
                routeDetails.nextSectionId
            }
        }
    }
}
