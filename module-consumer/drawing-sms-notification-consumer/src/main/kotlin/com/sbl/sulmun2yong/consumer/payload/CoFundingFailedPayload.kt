package com.sbl.sulmun2yong.consumer.payload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoFundingFailedPayload(
    val eventId: String,
    val fundingId: String,
)
