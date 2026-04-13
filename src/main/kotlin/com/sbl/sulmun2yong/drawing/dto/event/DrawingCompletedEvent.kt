package com.sbl.sulmun2yong.drawing.dto.event

import java.time.Instant

data class DrawingCompletedEvent(
    val eventId: String,
    val surveyId: String,
    val participantId: String,
    val selectedNumber: Int,
    val isWinner: Boolean,
    val rewardName: String?,
    val rewardCategory: String?,
    val remainingTickets: Int,
    val timestamp: Instant = Instant.now(),
)
