package com.sbl.sulmun2yong.cofunding.entity

import jakarta.persistence.*
import java.util.*

// co_funding_participants 슬림 사본 - 장벽 서브쿼리(SETTLED 카운트)와
// 환불 팬아웃 조회에 필요한 컬럼만 매핑한다 (원본: modules-web :support)
@Entity
@Table(name = "co_funding_participants")
class CoFundingParticipant(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val fundingId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: CoFundingParticipantStatus,

    @Column(name = "order_id", nullable = false, length = 64)
    val tossOrderId: String,
)
