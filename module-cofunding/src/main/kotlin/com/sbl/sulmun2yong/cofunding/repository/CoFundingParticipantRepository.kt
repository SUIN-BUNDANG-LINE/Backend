package com.sbl.sulmun2yong.cofunding.repository

import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CoFundingParticipantRepository : JpaRepository<CoFundingParticipant, UUID> {
    fun findByFundingIdAndUserId(
        fundingId: UUID,
        userId: UUID,
    ): CoFundingParticipant?

    fun findByTossOrderId(tossOrderId: String): CoFundingParticipant?

    fun findAllByFundingIdAndStatus(
        fundingId: UUID,
        status: CoFundingParticipantStatus,
    ): List<CoFundingParticipant>

    fun findAllByFundingId(fundingId: UUID): List<CoFundingParticipant>
}
