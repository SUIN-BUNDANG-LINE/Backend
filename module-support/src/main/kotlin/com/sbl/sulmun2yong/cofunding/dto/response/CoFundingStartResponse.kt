package com.sbl.sulmun2yong.cofunding.dto.response

import java.time.LocalDateTime
import java.util.*

data class CoFundingStartResponse(
    val fundingId: UUID,
    val sharedAmount: Int,
    val ownerShareAmount: Int,
    val deadline: LocalDateTime,
    // 참여자에게 공유하는 결제 진입 페이지 - 프론트가 이 페이지에서 내 주문 조회 API 를 부른다
    val inviteUrl: String,
)
