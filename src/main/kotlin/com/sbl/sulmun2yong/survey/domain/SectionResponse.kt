package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.domain.question.ResponseCommand
import java.util.UUID

data class SectionResponse(
    val questionId: UUID,
    val questionResponses: ResponseCommand,
)
