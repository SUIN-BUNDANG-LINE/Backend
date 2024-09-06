package com.sbl.sulmun2yong.survey.domain.result

import java.util.Date
import java.util.UUID

data class SurveyResult(
    val responses: List<Response>,
) {
    data class Response(
        val questionId: UUID,
        val participantId: UUID,
        val content: String,
        val createdAt: Date,
    )
}
