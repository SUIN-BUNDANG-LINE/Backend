package com.sbl.sulmun2yong.survey.domain.response

import com.sbl.sulmun2yong.survey.exception.InvalidSurveyResponseException
import java.util.UUID

data class SurveyResponse(
    val surveyId: UUID,
    val visitorId: String,
    private val sectionResponses: List<SectionResponse>,
) : List<SectionResponse> by sectionResponses {
    init {
        require(isSectionResponsesUnique()) { throw InvalidSurveyResponseException() }
    }

    private fun isSectionResponsesUnique() = sectionResponses.map { it.sectionId }.toSet().size == sectionResponses.size
}
