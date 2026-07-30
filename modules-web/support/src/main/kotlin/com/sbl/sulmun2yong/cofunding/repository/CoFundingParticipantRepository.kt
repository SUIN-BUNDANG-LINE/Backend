package com.sbl.sulmun2yong.cofunding.repository

import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CoFundingParticipantRepository : JpaRepository<CoFundingParticipant, UUID> {
    // 내 주문 조회 - 본인 참여자 행(사전 발급된 orderId 보유)
    fun findByFundingIdAndUserId(
        fundingId: UUID,
        userId: UUID,
    ): CoFundingParticipant?

    // settle(D6)/릴레이 CANCEL 후처리 - 주문 -> 참여자 역참조
    fun findByOrderId(orderId: String): CoFundingParticipant?
}
