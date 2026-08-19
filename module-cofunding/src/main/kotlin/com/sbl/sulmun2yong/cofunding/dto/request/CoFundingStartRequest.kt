package com.sbl.sulmun2yong.cofunding.dto.request

import java.time.LocalDateTime
import java.util.*

data class CoFundingStartRequest(
    // 개설자 자신을 포함한 참여자 명단 - capacity = 명단 크기
    val participantUserIds: List<UUID>,
    val deadline: LocalDateTime,
)
