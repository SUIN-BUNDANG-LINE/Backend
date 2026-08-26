package com.sbl.sulmun2yong.payment.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

// PG로 보낼 명령을 도메인 변경과 한 트랜잭션으로 적재해 at-least-once 발송을 보장한다.
@Entity
@Table(name = "toss_api_call_outbox")
class TossApiCallOutboxEntity(
    // 엔티티 PK 겸 PG 멱등키 — DB auto-increment가 아니라 앱에서 UUID 생성
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val callType: TossApiCallType,

    // 상관 키 — 커맨드가 어느 결제 주문에 속하는지(toss_orders.id = TossOrderEntity PK 와 같은 값)
    @Column(name = "order_id", nullable = false, length = 64)
    val tossOrderId: String,

    // PG 요청 본문 JSON (confirm: paymentKey & orderId & amount)
    @Column(nullable = false, columnDefinition = "TEXT")
    val requestPayload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TossApiCallStatus = TossApiCallStatus.PENDING,

    @Column(nullable = false)
    private var retryCount: Int = 0,

    val paymentKey: String,

    // 릴레이가 다시 집는 기준이 된다
    val createdAt: Instant,

    var succeededAt: Instant? = null,
) {
    // 재시도 소진 전용 - 정상 종착 도장은 리포지토리 CAS(trySucceed/tryFail)가 담당
    fun markFailed() {
        this.status = TossApiCallStatus.FAILED
    }

    // 멱등키 회차 - 조회로 미완료를 확정한 뒤에만 증가한다
    val retry: Int
        get() = retryCount

    fun incrementRetry(): Boolean {
        retryCount++
        if (retryCount >= MAX_RETRY_COUNT) {
            markFailed()
            return true
        }
        return false
    }

    companion object {
        const val MAX_RETRY_COUNT = 5

        fun create(
            callType: TossApiCallType,
            tossOrderId: String,
            requestPayload: String,
            paymentKey: String,
        ) = TossApiCallOutboxEntity(
            id = UUID.randomUUID(),
            callType = callType,
            tossOrderId = tossOrderId,
            requestPayload = requestPayload,
            createdAt = Instant.now(),
            paymentKey = paymentKey,
        )
    }
}
