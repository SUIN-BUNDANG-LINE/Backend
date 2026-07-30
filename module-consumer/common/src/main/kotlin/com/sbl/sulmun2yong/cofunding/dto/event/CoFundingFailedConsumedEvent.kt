package com.sbl.sulmun2yong.cofunding.dto.event

// `co-fundng-failed` (groupId: co-funding-refund)를 Kafka 계층에서 받아
// 도메인 계층으로 넘기는 내부 이벤트.
data class CoFundingFailedConsumedEvent(
    val eventId: String,
    val fundingId: String,
)
