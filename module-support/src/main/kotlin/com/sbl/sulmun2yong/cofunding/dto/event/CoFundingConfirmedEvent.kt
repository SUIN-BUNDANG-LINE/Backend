package com.sbl.sulmun2yong.cofunding.dto.event

import java.time.Instant

// ⑤ co-funding-confirmed wire 계약 - 전원 SETTLED 장벽 통과 사실 (발행: 장벽 CAS 승자의 Outbox).
// 구독: 설문(활성화 - surveys 는 설문만 쓴다). 장벽 승자가 설문을 직접 활성화하던 것을 대체한다.
data class CoFundingConfirmedEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val confirmedAt: Instant,
)
