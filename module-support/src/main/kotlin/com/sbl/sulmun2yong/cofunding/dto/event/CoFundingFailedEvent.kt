package com.sbl.sulmun2yong.cofunding.dto.event

import java.time.Instant

// co-funding-failed wire 계약 - 무산 확정 (발행: 기한 스케줄러의 CAS 승자, 구독: 환불 리스너).
// settledOrderIds 는 발행 시점 스냅샷 - 환불 리스너는 DB의 SETTLED 행 재조회가 진실 (D6 경계).
// module-consumer/common에 동일 필드 사본을 유지한다 (D11).
data class CoFundingFailedEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val settledOrderIds: List<String>,
    val failedAt: Instant,
)
