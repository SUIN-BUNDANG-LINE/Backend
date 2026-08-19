package com.sbl.sulmun2yong.cofunding.dto.response

import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import java.util.*

// 개설 접수 응답 - 접수증. 자격은 "서버가 이 순간 새로 만든 정보"뿐이다:
// fundingId(폴링 좌표)와 status(접수 사실). deadline 은 요청의 에코라 싣지 않고,
// inviteUrl 은 확정(FUNDING)의 산물이라 상태 조회 응답이 확정 후에 준다(노출 게이트).
data class CoFundingStartResponse(
    val fundingId: UUID,
    val status: CoFundingStatus,
)
