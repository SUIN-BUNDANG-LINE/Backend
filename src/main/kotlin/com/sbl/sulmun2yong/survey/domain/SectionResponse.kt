package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.QuestionResponse
import java.util.UUID

data class SectionResponse(
    val sectionId: UUID,
    val questionResponses: List<QuestionResponse>,
) : List<QuestionResponse> by questionResponses
