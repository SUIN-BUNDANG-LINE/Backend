package com.sbl.sulmun2yong.payment.service

import com.sbl.sulmun2yong.payment.entity.PaymentOrderStatus
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// failUrl 처리 - 사용자가 결제창에서 실패/이탈한 경우. 장부 FAILED + 설문 복귀 (멱등).
@Service
class PaymentFailService(
    private val paymentOrderRepository: PaymentOrderRepository,
    private val surveyRepository: SurveyRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentFailService::class.java)
    }

    @Transactional
    fun handleFail(
        orderId: String,
        code: String?,
    ) {
        val order = paymentOrderRepository.findByOrderId(orderId).orElse(null) ?: return

        // 이미 확정됨 - 늦은 fail은 무시 (멱등)
        if (order.status != PaymentOrderStatus.PENDING) return

        order.markFailed()
        log.warn("결제창 실패/이탈 - orderId={}, code={}", orderId, code)

        val survey =
            surveyRepository.findByIdAndIsDeletedFalse(order.surveyId).orElse(null) ?: return

        if (survey.status == SurveyStatus.PENDING_PAYMENT) {
            surveyRepository.save(survey.revertToNotStarted())
        }

    }
}
