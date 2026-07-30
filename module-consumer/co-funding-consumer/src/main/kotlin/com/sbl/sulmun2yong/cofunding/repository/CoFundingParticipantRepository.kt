package com.sbl.sulmun2yong.cofunding.repository

import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CoFundingParticipantRepository : JpaRepository<CoFundingParticipant, UUID> {
    // 환불 팬아웃 대상 - 이벤트 스냅샷이 아니라 이 DB 재조회 결과가 CANCEL 적재의 진실
    fun findAllByFundingIdAndStatus(
        fundingId: UUID,
        status: CoFundingParticipantStatus,
    ): List<CoFundingParticipant>
}
