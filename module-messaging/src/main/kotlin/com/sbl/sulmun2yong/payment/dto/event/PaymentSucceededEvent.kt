package com.sbl.sulmun2yong.payment.dto.event

import com.sbl.sulmun2yong.global.kafka.DomainEvent
import java.time.Instant

// payment-succeeded wire 계약 - confirm 성공 사실 (발행: settle tx 의 Outbox).
// 구독: 모금(참여자 PAID + 장벽 CAS - 승자만 ⑤ 발행). 설문 개시는 장벽을 거친 ⑤의 몫이다.
data class PaymentSucceededEvent(
    override val eventId: String,
    val orderId: String,
    // 산 물건의 좌표 - (타입 = 해석할 도메인, id = 그 테이블의 행). 소비자는 자기 타입만 걸러 반응한다.
    val productType: String = "DRAWING_BOARD",
    val productId: String,
    val succeededAt: Instant,
) : DomainEvent
