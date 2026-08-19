package com.sbl.sulmun2yong.cofunding.dto.event

import com.sbl.sulmun2yong.global.kafka.DomainEvent
import java.time.Instant

// co-funding-created wire 계약 - 개설 확정 사실 (발행: 개설 tx의 Outbox).
// 구독: 결제(참여자별 payment_orders 생성) · 설문(PENDING_PAYMENT 전이) · 추첨(경품 보드 생성).
// 단일 기록자 - 개설 tx는 co_fundings·participants만 쓰고, 나머지 쓰기는 이 이벤트로 수렴한다.
data class CoFundingCreatedEvent(
    override val eventId: String,
    val fundingId: String,
    // 산 물건(결제 대기 보드)의 ID - 참여자 주문들이 공동으로 가리킨다 (②에서 반사).
    // 설문 좌표는 싣지 않는다 - 유일 구독자(결제)가 보드 좌표만 소비한다.
    val boardId: String,
    val participants: List<Participant>,
    val createdAt: Instant,
) : DomainEvent {
    // orderId·amount 는 모금이 개설 시점에 확정한 값 - 결제는 받은 대로 주문을 만든다
    data class Participant(
        val userId: String,
        val orderId: String,
        val amount: Int,
    )
}
