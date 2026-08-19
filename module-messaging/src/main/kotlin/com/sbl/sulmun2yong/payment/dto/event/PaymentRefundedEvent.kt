package com.sbl.sulmun2yong.payment.dto.event

import com.sbl.sulmun2yong.global.kafka.DomainEvent
import java.time.Instant

// ⑦ payment-refunded wire 계약 - 환불(CANCEL) 완료 사실 (발행: 릴레이 CANCEL 승인 tx 의 Outbox).
// 구독: 모금(참여자 REFUNDED 전이 + FAILED→REFUNDED 수렴 CAS - participants·co_fundings 는 모금만 쓴다).
data class PaymentRefundedEvent(
    override val eventId: String,
    val orderId: String,
    // 산 물건의 좌표 - (타입 = 해석할 도메인, id = 그 테이블의 행). 소비자는 자기 타입만 걸러 반응한다.
    val productType: String = "DRAWING_BOARD",
    val productId: String,
    val refundedAt: Instant,
) : DomainEvent
