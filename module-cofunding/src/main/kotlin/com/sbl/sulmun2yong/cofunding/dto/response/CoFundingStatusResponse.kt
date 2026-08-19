package com.sbl.sulmun2yong.cofunding.dto.response

import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import java.time.LocalDateTime
import java.util.*

// 모금 상태 조회 응답 - 접수(202 성격) 후 판정 확정을 폴링하는 프론트 계약.
// 분담금은 승인 전 0 - status 가 FUNDING 이후일 때만 유효하다.
// inviteUrl 도 확정의 산물 - 승인 전(PENDING·REJECTED)엔 null(거절될 모금의 초대가 뿌려지는 것 방지).
data class CoFundingStatusResponse(
    val fundingId: UUID,
    val status: CoFundingStatus,
    val sharedAmount: Int,
    val deadline: LocalDateTime,
    val inviteUrl: String? = null,
)
