package com.sbl.sulmun2yong.drawing.dto.event

import java.time.LocalDateTime
import java.util.UUID

data class WinningEvent(
    val drawingHistoryId: UUID,
    val surveyId: UUID,
    val surveyMakerId: UUID,
    val rewardName: String,
    val phoneNumber: String,
    val timestamp: LocalDateTime,
)
