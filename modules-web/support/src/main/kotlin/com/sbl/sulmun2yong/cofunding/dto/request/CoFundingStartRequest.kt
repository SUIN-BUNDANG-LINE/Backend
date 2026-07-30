package com.sbl.sulmun2yong.cofunding.dto.request

import java.time.LocalDateTime
import java.util.*

data class CoFundingStartRequest(
    // 개설자 제외 초대 명단 - capacity = 명단 + 1(개설자)
    val participantUserIds: List<UUID>,
    val deadline: LocalDateTime,
)
