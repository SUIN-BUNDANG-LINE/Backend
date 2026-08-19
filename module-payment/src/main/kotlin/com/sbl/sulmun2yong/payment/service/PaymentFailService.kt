package com.sbl.sulmun2yong.payment.service

import com.sbl.sulmun2yong.payment.entity.TossOrderStatus
import com.sbl.sulmun2yong.payment.publisher.PaymentEventPublisher
import com.sbl.sulmun2yong.payment.repository.TossOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// failUrl 처리 - 사용자가 결제창에서 실패/이탈한 경우. 장부 FAILED 기록 (멱등).
// 설문 복귀는 설문 리스너 몫(단일 기록자) - 결제는 surveys 를 쓰지 않는다.
@Service
class PaymentFailService(
    private val tossOrderRepository: TossOrderRepository,
    private val paymentEventPublisher: PaymentEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentFailService::class.java)
    }

    @Transactional
    fun handleFail(
        orderId: String,
        code: String?,
    ) {
        val order = tossOrderRepository.findById(orderId).orElse(null) ?: return

        if (order.status != TossOrderStatus.PENDING) return

        order.markFailed()
        log.warn("결제창 실패/이탈 - orderId={}, code={}", orderId, code)
    }
}
