package com.sbl.sulmun2yong.cofunding.entity

import com.sbl.sulmun2yong.cofunding.exception.InvalidCoFundingStateException
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

// 모금 참여자 - REGISTERED(초대 확정·미결제) -> PAID(결제 확정) -> REFUNDED(무산 환불).
// PAID 행 수가 장벽 판정의 근거다. UNIQUE(funding_id, user_id)로 명단 중복 차단.
@Entity
@Table(name = "co_funding_participants")
class CoFundingParticipant(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val fundingId: UUID,

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CoFundingParticipantStatus = CoFundingParticipantStatus.REGISTERED,

    @Column(name = "order_id", nullable = false, length = 64)
    val tossOrderId: String,

    var paidAt: LocalDateTime? = null,
) : BaseTimeEntity() {
    fun markPaid(now: LocalDateTime) {
        if (status != CoFundingParticipantStatus.REGISTERED) throw InvalidCoFundingStateException()
        status = CoFundingParticipantStatus.PAID
        paidAt = now
    }

    fun markRefunded() {
        if (status != CoFundingParticipantStatus.PAID) throw InvalidCoFundingStateException()
        status = CoFundingParticipantStatus.REFUNDED
    }

    companion object {
        fun of(
            fundingId: UUID,
            userId: UUID,
            orderId: String,
        ) = CoFundingParticipant(
            id = UUID.randomUUID(),
            fundingId = fundingId,
            userId = userId,
            tossOrderId = orderId,
        )
    }
}
