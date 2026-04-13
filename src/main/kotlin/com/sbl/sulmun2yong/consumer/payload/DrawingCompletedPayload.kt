package com.sbl.sulmun2yong.consumer.payload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DrawingCompletedPayload(
    val eventId: String,
    val surveyId: String,
    val isWinner: Boolean,
    val remainingTickets: Int,
)
