package com.sbl.sulmun2yong.survey.domain.section

import com.sbl.sulmun2yong.survey.domain.question.Question
import com.sbl.sulmun2yong.survey.domain.question.SingleChoiceQuestion
import com.sbl.sulmun2yong.survey.domain.routing.RouteDetails
import com.sbl.sulmun2yong.survey.exception.InvalidSectionException
import com.sbl.sulmun2yong.survey.exception.InvalidSectionResponseException
import java.util.UUID

data class Section(
    val id: SectionId.Standard,
    val title: String,
    val description: String,
    val routeDetails: RouteDetails,
    val questions: List<Question>,
    val sectionIds: SectionIds,
) {
    init {
        validateSection()
    }

    private fun validateSection() {
        if (routeDetails is RouteDetails.SetByChoiceRouting) {
            val keyQuestion = findKeyQuestion() ?: throw InvalidSectionException()
            require(keyQuestion.getChoiceSet() == routeDetails.getChoiceSet()) { throw InvalidSectionException() }
        }
        require(sectionIds.isContainsAll(routeDetails.getNextSectionIds())) { throw InvalidSectionException() }
    }

    companion object {
        fun create(): Section {
            val id = UUID.randomUUID()
            return Section(
                id = SectionId.Standard(id),
                title = "",
                description = "",
                routeDetails = RouteDetails.NumericalOrderRouting,
                questions = emptyList(),
                sectionIds = SectionIds(listOf(SectionId.Standard(id), SectionId.End)),
            )
        }
    }

    fun findNextSectionId(sectionResponse: SectionResponse): SectionId {
        validateResponse(sectionResponse)
        return when (routeDetails) {
            is RouteDetails.SetByUserRouting -> routeDetails.nextSectionId
            is RouteDetails.SetByChoiceRouting -> routeDetails.findNextSectionId(sectionResponse)
            is RouteDetails.NumericalOrderRouting -> sectionIds.findNextSectionId(sectionResponse.sectionId)
        }
    }

    private fun findKeyQuestion(): SingleChoiceQuestion? {
        val setByChoiceRouting = routeDetails as? RouteDetails.SetByChoiceRouting ?: return null
        val question = questions.find { it.id == setByChoiceRouting.keyQuestionId } ?: return null
        return if (question.canBeKeyQuestion()) question as SingleChoiceQuestion else null
    }

    private fun validateResponse(sectionResponse: SectionResponse) {
        questions.forEach { question ->
            val response = sectionResponse.find { it.questionId == question.id }
            if (question.isRequired && response == null) throw InvalidSectionResponseException()
            require(response == null || question.isValidResponse(response)) { throw InvalidSectionResponseException() }
        }
    }
}
