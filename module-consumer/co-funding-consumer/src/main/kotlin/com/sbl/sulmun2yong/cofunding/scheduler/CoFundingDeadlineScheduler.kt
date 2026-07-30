package com.sbl.sulmun2yong.cofunding.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedEvent
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.global.kafka.publisher.KafkaEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

// 모금 기한 스케줄러(D8) - 만료된 FUNDING 을 스캔해 무산 CAS 를 수행하고, 승자만 failed 를 발행한다.
// 발행은 tx 밖(커밋 후): tx 안에서 발행하면 커밋 실패 시 "FUNDING 인데 failed" 유령 신호가 된다.
// 반대 창(커밋 후/발행 전 크래시)의 이벤트 유실은 수용 - 해당 모금은 FAILED 인 채 환불
// 미발동으로 남아 수동 복구 영역이다 (T011 관찰 항목).
@Component
class CoFundingDeadlineScheduler(
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val kafkaEventPublisher: KafkaEventPublisher,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingDeadlineScheduler::class.java)
        private const val TOPIC = "co-funding-failed"
        private const val BATCH_SIZE = 20
    }

    private val transactionTemplate = TransactionTemplate(transactionManager)

    @Scheduled(fixedDelay = 60_000)
    fun expireOverdueFundings() {
        val events = transactionTemplate.execute { claimAndFail() } ?: emptyList()
        events.forEach { event ->
            kafkaEventPublisher.publish(TOPIC, event.fundingId, objectMapper.writeValueAsString(event))
        }
        if (events.isNotEmpty()) log.info("기한 만료 무산 확정 {}건 - failed 발행", events.size)
    }

    // 짧은 tx: 만료 FUNDING 클레임(SKIP LOCKED) -> 무산 CAS -> 발행할 이벤트 조립.
    // 행 잠금이 settle 의 findByIdForUpdate 와 직렬화되어 "만료 vs 마지막 결제"는 한쪽만 이긴다.
    private fun claimAndFail(): List<CoFundingFailedEvent> {
        val now = LocalDateTime.now()
        return coFundingRepository
            .findExpiredForUpdateSkipLocked(now, PageRequest.of(0, BATCH_SIZE))
            .filter { coFundingRepository.tryFail(it.id, now) == 1 }
            .map { funding ->
                val settledOrderIds =
                    coFundingParticipantRepository
                        .findAllByFundingIdAndStatus(funding.id, CoFundingParticipantStatus.SETTLED)
                        .map { it.tossOrderId }

                CoFundingFailedEvent(
                    eventId = UUID.randomUUID().toString(),
                    fundingId = funding.id.toString(),
                    surveyId = funding.surveyId.toString(),
                    settledOrderIds = settledOrderIds,
                    failedAt = Instant.now(),
                )
            }
    }
}
