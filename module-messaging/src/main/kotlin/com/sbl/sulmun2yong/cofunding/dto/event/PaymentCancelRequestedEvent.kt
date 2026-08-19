package com.sbl.sulmun2yong.cofunding.dto.event

import com.sbl.sulmun2yong.global.kafka.DomainEvent
import java.time.Instant

// ⑧ payment-cancel-requested wire 계약 - 늦은 결제 단건 환불 명령 (발행: 모금 ④ 리스너의 Outbox).
// 무산(FAILED·REFUNDED) 후 도착한 결제 확정을 발견한 모금이 결제에 환불을 명령한다.
// 구독: 결제(CANCEL 커맨드 적재 - toss_api_call_outbox 는 결제만 쓴다).
data class PaymentCancelRequestedEvent(
    override val eventId: String,
    val orderId: String,
    val requestedAt: Instant,
) : DomainEvent
