package com.sbl.sulmun2yong.payment.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*

// Command Outbox(심부름 목록) - PG로 보낼 명령을 도메인 변경과 한 트랜잭션으로 적재해 신뢰성 발송을 보장한다.
// id를 앱에서 생성해 Idempotency-Key로 그대로 쏘낟 -> 재발송해도 PG가 1회만 처리(이중 결제 차단).
@Entity
@Table(name = "payment_commands")
class PaymentCommand(
    // 엔티티 PK 겸 PG 멱등키 — DB auto-increment가 아니라 앱에서 UUID 생성
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val commandType: PaymentCommandType,

    // 상관 키(orderId) — 커맨드가 어느 결제 주문에 속하는지
    val aggregateId: String,

    // PG 요청 본문 JSON (confirm: paymentKey & orderId & amount)
    @Column(nullable = false, columnDefinition = "TEXT")
    val requestPayload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentCommandStatus = PaymentCommandStatus.PENDING,

    @Column(nullable = false)
    var retryCount: Int = 0,

    // 릴레이 클레임 기준(stale 판정) - 직접 관리(Outbox 계열 관례)
    val createdAt: Instant,
    var sentAt: Instant? = null,
    var confirmedAt: Instant? = null,
) {
    // 릴레이 & 핸들러가 PG로 발송한 직후 - 결과는 아직 모름
    fun markSent() {
        this.status = PaymentCommandStatus.SENT
        this.sentAt = Instant.now()
    }

    // 승인 확정 (Approved 또는 조회 수렴)
    fun markConfirmed() {
        this.status = PaymentCommandStatus.CONFIRMED
        this.confirmedAt = Instant.now()
    }

    // 명시적 거절 확정 또는 재시도 소진
    fun markFailed() {
        this.status = PaymentCommandStatus.FAILED
    }

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
            commandType: PaymentCommandType,
            aggregateId: String,
            requestPayload: String,
        ) = PaymentCommand(
            id = UUID.randomUUID(),
            commandType = commandType,
            aggregateId = aggregateId,
            requestPayload = requestPayload,
            createdAt = Instant.now(),
        )
    }
}
