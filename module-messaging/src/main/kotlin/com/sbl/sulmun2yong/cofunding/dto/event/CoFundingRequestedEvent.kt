package com.sbl.sulmun2yong.cofunding.dto.event

import com.sbl.sulmun2yong.global.kafka.DomainEvent
import java.time.Instant

// co-funding-requested wire 계약 - 개설 접수 사실 (발행: 개설 tx 의 Outbox).
// 구독: 설문(판정 리스너 - 소유자·상태·경품 설정을 자기 트랜잭션에서 검증하고 승인/거절을 회신).
// 모금은 설문을 읽지 않는다 - 판정 권한이 데이터 소유자(설문)에게 있다.
data class CoFundingRequestedEvent(
    override val eventId: String,
    val fundingId: String,
    val surveyId: String,
    val ownerId: String,
    val requestedAt: Instant,
) : DomainEvent
