package com.sbl.sulmun2yong.survey.domain

import java.util.UUID

data class SurveyResponse(
    val sectionId: UUID,
    val sectionResponses: List<SectionResponse>,
)
