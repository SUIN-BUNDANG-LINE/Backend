package com.sbl.sulmun2yong.cofunding.scheduler

import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingSagaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// 모금 기한 스케줄러(D8) - 만료된 FUNDING 을 스캔해 무산 CAS 를 수행하고, 승자만 ⑥ failed 를 발행한다.
// 컨슈머 시절의 "tx 밖 직접 발행"(유령 신호 방지, 유실 수용)을 Outbox 로 대체 - tx 안 '적재'라
// 유령 신호가 없고, 릴레이가 발행을 보장해 유실도 없다(수동 복구 영역이 사라짐).
// 결제자 0명 무산은 CANCEL 이 없어 ⑦ 수렴이 발동할 수 없으므로 여기서 직접 종착시킨다(자기 테이블 - D5 예외).
@Component
class CoFundingDeadlineScheduler(
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val sagaPublisher: CoFundingSagaPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingDeadlineScheduler::class.java)
        private const val BATCH_SIZE = 20
    }

    // 짧은 tx: 만료 FUNDING 클레임(SKIP LOCKED) -> 무산 CAS -> ⑥ Outbox 적재.
    // 행 잠금이 ④ 리스너의 findByIdForUpdate 와 직렬화되어 "만료 vs 마지막 결제"는 한쪽만 이긴다.
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun expireOverdueFundings() {
        val now = LocalDateTime.now()
        val failed =
            coFundingRepository
                .findExpiredForUpdateSkipLocked(now, PageRequest.of(0, BATCH_SIZE))
                .filter { coFundingRepository.tryFail(it.id, now) == 1 }

        failed.forEach { funding ->
            val settledOrderIds =
                coFundingParticipantRepository
                    .findAllByFundingIdAndStatus(funding.id, CoFundingParticipantStatus.SETTLED)
                    .map { it.tossOrderId }

            if (settledOrderIds.isEmpty()) {
                // 결제자 0명 무산 - 환불할 CANCEL 이 없어 ⑦ 이 오지 않으므로 즉시 FAILED -> REFUNDED
                if (coFundingRepository.tryMarkRefunded(funding.id, now) == 1) {
                    log.info("결제자 0명 무산 - 즉시 종착: fundingId={}", funding.id)
                }
            } else {
                sagaPublisher.publishFailed(funding, settledOrderIds)
            }
        }
        if (failed.isNotEmpty()) log.info("기한 만료 무산 확정 {}건", failed.size)
    }

    // 판정 회신이 영영 오지 않는 접수(PENDING_APPROVAL)의 안전망 - 기한이 지나면 REJECTED 종착.
    // 결제자가 없는 상태라 환불이 없다 - CAS(tryRejectExpired)가 재실행·다중 인스턴스 멱등을 보장.
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun expireOverduePendingApprovals() {
        val expired = coFundingRepository.rejectExpiredPendingApprovals(LocalDateTime.now())
        if (expired > 0) log.info("판정 미회신 접수 만료 종착 {}건", expired)
    }
}
