package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

data class MyPageSurveyInfoResponse(
    val id: UUID,
    val title: String,
    val thumbnail: String?,
    val updatedAt: LocalDateTime,
    val status: SurveyStatus,
    val finishedAt: Date?,
    val responseCount: Int,
)
