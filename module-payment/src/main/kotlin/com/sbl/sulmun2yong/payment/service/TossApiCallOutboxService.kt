package com.sbl.sulmun2yong.payment.service

import com.sbl.sulmun2yong.payment.entity.TossApiCallOutboxEntity
import com.sbl.sulmun2yong.payment.entity.TossApiCallStatus
import com.sbl.sulmun2yong.payment.entity.TossApiCallType
import com.sbl.sulmun2yong.payment.repository.TossApiCallOutboxRepository
import com.sbl.sulmun2yong.payment.repository.TossOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class TossApiCallOutboxService(
    private val tossApiCallOutboxRepository: TossApiCallOutboxRepository,
    private val tossOrderRepository: TossOrderRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(TossApiCallOutboxService::class.java)
    }

    @Transactional
    fun commitConfirmIntent(
        orderId: String,
        paymentKey: String,
        requestPayload: String,
    ): TossApiCallOutboxEntity {
        val existing =
            tossApiCallOutboxRepository
                .findByTossOrderIdAndCallTypeAndPaymentKey(
                    orderId,
                    TossApiCallType.CONFIRM,
                    paymentKey = paymentKey,
                )
        if (existing != null) return existing

        tossApiCallOutboxRepository.supersedePendingConfirm(orderId)
        return tossApiCallOutboxRepository.save(
            TossApiCallOutboxEntity.create(
                TossApiCallType.CONFIRM,
                orderId,
                paymentKey,
                requestPayload,
            ),
        )
    }

    @Transactional
    fun commitCancelIntent(
        orderId: String,
        requestPayload: String,
    ): Boolean {
        val paymentKey = tossOrderRepository.findById(orderId).orElse(null)?.paymentKey
        if (paymentKey == null) {
            log.error("승인 키가 없는 주문에 대해 취소 명령 발생, 로직을 점검해야합니다 - orderId={}", orderId)
            return false
        }
        if (tossApiCallOutboxRepository.existsByTossOrderIdAndCallTypeAndPaymentKey(
                orderId,
                TossApiCallType.CANCEL,
                paymentKey,
            )
        ) {
            return false
        }
        tossApiCallOutboxRepository.save(
            TossApiCallOutboxEntity.create(
                TossApiCallType.CANCEL,
                orderId,
                requestPayload,
                paymentKey,
            ),
        )
        return true
    }

    @Transactional
    fun claimPendingForRelay(
        limit: Int,
        staleSeconds: Long,
    ): List<TossApiCallOutboxEntity> =
        tossApiCallOutboxRepository.findPendingForUpdateSkipLocked(
            TossApiCallStatus.PENDING,
            Instant.now().minusSeconds(staleSeconds),
            PageRequest.of(0, limit),
        )

    @Transactional
    fun markSucceeded(callId: UUID): TossApiCallOutboxEntity? {
        if (tossApiCallOutboxRepository.trySucceed(callId, Instant.now()) == 0) {
            log.debug("이미 종착한 호출(멱등 스킵): {}", callId)
            return null
        }
        return tossApiCallOutboxRepository.findById(callId).orElseThrow()
    }

    @Transactional
    fun markFailed(callId: UUID): TossApiCallOutboxEntity? {
        if (tossApiCallOutboxRepository.tryFail(callId) == 0) return null
        return tossApiCallOutboxRepository.findById(callId).orElseThrow()
    }

    @Transactional
    fun incrementRetry(callId: UUID) {
        val call = tossApiCallOutboxRepository.findById(callId).orElseThrow()
        if (call.incrementRetry()) {
            log.error("토스 호출 재시도 소진 - orderId={} 수동 확인 필요", call.tossOrderId)
        }
    }
}
