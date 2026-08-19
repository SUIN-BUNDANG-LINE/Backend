package com.sbl.sulmun2yong.cofunding.scheduler

import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingOutboxKafkaPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class CoFundingDeadlineScheduler(
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val coFundingOutboxKafkaPublisher: CoFundingOutboxKafkaPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingDeadlineScheduler::class.java)
        private const val BATCH_SIZE = 20
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun expireOverdueFundings() {
        val now = LocalDateTime.now()
        val failed =
            coFundingRepository
                .findExpiredForUpdateSkipLocked(now, PageRequest.of(0, BATCH_SIZE))
                .filter { coFundingRepository.tryFail(it.id, now) == 1 }

        failed.forEach { funding ->
            val paidOrderIds =
                coFundingParticipantRepository
                    .findAllByFundingIdAndStatus(funding.id, CoFundingParticipantStatus.PAID)
                    .map { it.tossOrderId }

            if (paidOrderIds.isEmpty()) {
                // 결제한 사람이 없어서 환불 이벤트 발행 없이 즉시 FAILED 상태로 만듧니다
                if (coFundingRepository.tryMarkRefunded(funding.id, now) == 1) {
                    log.info("결제자 0명 무산 - 즉시 종착: fundingId={}", funding.id)
                }
            } else {
                coFundingOutboxKafkaPublisher.publishExpired(funding, paidOrderIds)
            }
        }
        if (failed.isNotEmpty()) log.info("기한 만료 무산 확정 {}건", failed.size)
    }

    // 판정 회신이 영영 오지 않는 접수(PENDING)의 안전망 - 기한이 지나면 REJECTED 종착.
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun expireOverduePendingApprovals() {
        val expired = coFundingRepository.rejectExpiredPendingApprovals(LocalDateTime.now())
        if (expired > 0) log.info("판정 미회신 접수 만료 종착 {}건", expired)
    }
}
