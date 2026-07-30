package com.sbl.sulmun2yong.cofunding.dto.event

import java.time.Instant

// co-payment-settled wire 계약 사본 - 원본은 modules-web :support cofunding/dto/event (D11).
// 필드 변경 시 contracts/event.md 를 계약 문서로 삼아 양쪽을 함께 고친다.
data class CoPaymentSettledEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val participantId: String,
    val orderId: String,
    val settledAt: Instant,
)
