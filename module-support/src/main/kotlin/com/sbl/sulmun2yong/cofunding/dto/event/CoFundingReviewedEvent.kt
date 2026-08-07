package com.sbl.sulmun2yong.cofunding.dto.event

import java.time.Instant

// co-funding-reviewed wire 계약 - 설문의 개설 판정 결과 (발행: 설문 판정 tx 의 Outbox).
// 한 판정의 두 결과(승인·거절)라 토픽 하나에 verdict 로 싣는다 - 발행자·구독자·사가 단계가 같고,
// fundingId 키라 같은 모금의 판정은 순서가 보장된다.
// 구독: 모금(승인 → 분담금 확정+FUNDING+② 발행 / 거절 → REJECTED 종착).
data class CoFundingReviewedEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val verdict: Verdict,
    // 승인일 때만 - 총액(단가 × 경품 수)은 설문 소유 데이터다
    val totalAmount: Int? = null,
    // 거절일 때만 - 소유자 불일치·이미 시작된 설문 등
    val reason: String? = null,
    val reviewedAt: Instant,
) {
    enum class Verdict {
        APPROVED,
        REJECTED,
    }
}
