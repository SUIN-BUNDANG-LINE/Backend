package com.sbl.sulmun2yong.cofunding.dto.event

import java.time.Instant

// co-payment-settled wire 계약 - 참여자 1명 결제 확정 (발행: settle tx의 Outbox, 구독: 집계 리스너).
// module-consumer/common 에 동일 필드 사본을 유지한다 (D11).
data class CoPaymentSettledEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val participantId: String,
    val orderId: String,
    val settledAt: Instant,
)
