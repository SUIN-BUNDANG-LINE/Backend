package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.Question
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
        validateSection()
    }

    private fun validateSection() {
        if (routeDetails is RouteDetails.SetByChoice) {
            val keyQuestion = findKeyQuestion() ?: throw InvalidSectionException()
            require(routeDetails.getContentsSet() == keyQuestion.getChoiceSet()) { throw InvalidSectionException() }
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

    fun getDestinationSectionIdSet() = routeDetails.getDestinationSectionIdSet()

    fun findNextSectionId(sectionResponse: SectionResponse): UUID? {
        validateSectionResponse(sectionResponse)
        return getNextSectionId(sectionResponse)
    }

    private fun validateSectionResponse(sectionResponse: SectionResponse) {
        questions.forEach { question ->
            val response = sectionResponse.find { it.questionId == question.id }
            require(!question.isRequired || response != null) { throw InvalidSectionResponseException() }
            if (question.isRequired && response == null) {
                throw InvalidSectionResponseException()
            }
            if (response != null && !question.isValidResponse(response)) {
                throw InvalidSectionResponseException()
            }
        }
    }

    private fun getNextSectionId(sectionResponse: SectionResponse): UUID? {
        return when (routeDetails) {
            is RouteDetails.SetByChoice -> {
                val keyQuestionId = routeDetails.keyQuestionId
                val keyQuestionResponse = sectionResponse.findQuestionResponse(keyQuestionId) ?: throw InvalidSectionResponseException()
                routeDetails.findNextSectionId(keyQuestionResponse.first())
            }
            is RouteDetails.NumericalOrder -> routeDetails.nextSectionId
            is RouteDetails.SetByUser -> routeDetails.nextSectionId
        }
    }

    private fun findKeyQuestion(): SingleChoiceQuestion? {
        val setByChoiceRouting = routeDetails as? RouteDetails.SetByChoice ?: return null
        val question = questions.find { it.id == setByChoiceRouting.keyQuestionId } ?: return null
        return if (question.canBeKeyQuestion()) question as SingleChoiceQuestion else null
    }
}
