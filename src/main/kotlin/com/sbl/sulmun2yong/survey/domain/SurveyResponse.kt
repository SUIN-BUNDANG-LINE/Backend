package com.sbl.sulmun2yong.survey.domain

import java.util.UUID

data class SurveyResponse(
    val surveyId: UUID,
    private val sectionResponses: List<SectionResponse>,
) : List<SectionResponse> by sectionResponses
