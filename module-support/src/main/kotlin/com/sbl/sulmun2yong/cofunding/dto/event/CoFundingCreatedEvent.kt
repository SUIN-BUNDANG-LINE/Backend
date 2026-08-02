package com.sbl.sulmun2yong.cofunding.dto.event

import java.time.Instant

// co-funding-created wire 계약 - 개설 확정 사실 (발행: 개설 tx의 Outbox).
// 구독: 결제(참여자별 payment_orders 생성) · 설문(PENDING_PAYMENT 전이) · 추첨(경품 보드 생성).
// 단일 기록자 - 개설 tx는 co_fundings·participants만 쓰고, 나머지 쓰기는 이 이벤트로 수렴한다.
data class CoFundingCreatedEvent(
    val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val participants: List<Participant>,
    val createdAt: Instant,
) {
    // orderId·amount 는 모금이 개설 시점에 확정한 값 - 결제는 받은 대로 주문을 만든다
    data class Participant(
        val userId: String,
        val orderId: String,
        val amount: Int,
    )
}
