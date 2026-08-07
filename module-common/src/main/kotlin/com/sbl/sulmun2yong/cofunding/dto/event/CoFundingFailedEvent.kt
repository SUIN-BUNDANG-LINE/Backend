package com.sbl.sulmun2yong.cofunding.dto.event

import java.time.Instant

// co-funding-failed wire 계약 사본 - 원본은 modules-web :support cofunding/dto/event (D11).
// settledOrderIds 는 발행 시점 스냅샷 - 환불 리스너는 DB 의 SETTLED 재조회가 진실 (D6 경계).
data class CoFundingFailedEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val settledOrderIds: List<String>,
    val failedAt: Instant,
)
