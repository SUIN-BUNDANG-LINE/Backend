package com.sbl.sulmun2yong.survey.dto.event

import java.time.LocalDateTime
import java.util.UUID

data class SurveyResponseEvent(
    val participantId: UUID,
    val surveyId: UUID,
    val surveyMakerId: UUID,
    val timestamp: LocalDateTime,
)
