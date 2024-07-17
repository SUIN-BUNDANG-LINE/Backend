package com.sbl.sulmun2yong.survey.domain

import java.util.UUID

data class SectionResponse(
    val sectionId: UUID,
    val questionResponses: List<QuestionResponse>,
)
