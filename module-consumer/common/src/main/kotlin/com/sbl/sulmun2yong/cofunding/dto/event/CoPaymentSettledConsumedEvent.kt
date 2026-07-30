package com.sbl.sulmun2yong.cofunding.dto.event

// `co-payment-settled`(groupId: co-funding-settlement)를 Kafka 계층에서 받아
data class CoPaymentSettledConsumedEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
)
