package com.sbl.sulmun2yong.cofunding.dto.response

import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import java.time.LocalDateTime
import java.util.*

// 개설 접수 응답 - 보증 범위는 "접수(PENDING_APPROVAL)"까지.
// 분담금은 설문 판정 승인에서 확정되므로 여기 없다 - 프론트는 상태 조회로 FUNDING 확정과 금액을 받는다.
data class CoFundingStartResponse(
    val fundingId: UUID,
    val status: CoFundingStatus,
    val deadline: LocalDateTime,
    // 참여자에게 공유하는 결제 진입 페이지 - 프론트가 이 페이지에서 내 주문 조회 API 를 부른다
    val inviteUrl: String,
)
