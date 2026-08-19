package com.sbl.sulmun2yong.cofunding.dto.event

import com.sbl.sulmun2yong.global.kafka.DomainEvent
import java.time.Instant

// co-funding-expired wire 계약 - 기한 만료 무산 확정 (발행: 기한 스케줄러의 CAS 승자, 구독: 환불 리스너).
// paidOrderIds 는 발행 시점 스냅샷 - 환불 리스너는 DB의 PAID 행 재조회가 진실 (D6 경계).
// module-consumer/common에 동일 필드 사본을 유지한다 (D11).
data class CoFundingExpiredEvent(
    override val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val paidOrderIds: List<String>,
    val expiredAt: Instant,
) : DomainEvent
