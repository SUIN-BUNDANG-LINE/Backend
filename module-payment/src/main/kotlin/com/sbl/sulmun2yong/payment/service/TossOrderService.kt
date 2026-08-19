package com.sbl.sulmun2yong.payment.service

import com.sbl.sulmun2yong.payment.publisher.PaymentEventPublisher
import com.sbl.sulmun2yong.payment.repository.TossOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TossOrderService(
    private val tossOrderRepository: TossOrderRepository,
    private val tossApiCallOutboxService: TossApiCallOutboxService,
    private val paymentEventPublisher: PaymentEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(TossOrderService::class.java)
    }

    // 승인 확정 - 도장·장부 SUCCEEDED + ④ succeeded 발행, 한 tx
    @Transactional
    fun settleApproved(
        tossApiCallOutboxId: UUID,
        paymentKey: String,
    ) {
        val tossApiCallOutbox =
            tossApiCallOutboxService.markSucceeded(tossApiCallOutboxId) ?: return
        val order = tossOrderRepository.findById(tossApiCallOutbox.tossOrderId).orElseThrow()
        order.markSucceeded(paymentKey)
        log.info("결제 확정 - orderId={}, paymentKey={}", tossApiCallOutbox.tossOrderId, paymentKey)
        paymentEventPublisher.publishSucceeded(order)
    }

    @Transactional
    fun settleRejected(
        tossApiCallOutboxId: UUID,
        code: String,
    ) {
        val call = tossApiCallOutboxService.markFailed(tossApiCallOutboxId) ?: return
        val order = tossOrderRepository.findById(call.tossOrderId).orElseThrow()
        order.markFailed()
        log.warn("결제 거절 확정 - orderId={}, code={}", call.tossOrderId, code)
    }

    @Transactional
    fun settleCancelled(callId: UUID) {
        val call = tossApiCallOutboxService.markSucceeded(callId) ?: return
        val order = tossOrderRepository.findById(call.tossOrderId).orElseThrow()
        order.markCanceled()
        log.info("환불 확정 - orderId={}", call.tossOrderId)
        paymentEventPublisher.publishRefunded(order)
    }

    @Transactional(readOnly = true)
    fun findPaymentKeyByOrderId(orderId: String): String? = tossOrderRepository.findById(orderId).orElse(null)?.paymentKey
}
