package com.sbl.sulmun2yong.payment.service

import com.sbl.sulmun2yong.payment.entity.PaymentOrder
import com.sbl.sulmun2yong.payment.entity.PaymentOrderOrigin
import com.sbl.sulmun2yong.payment.entity.PaymentOrderStatus
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

// 단독 결제 주문 발급 - survey-payment-pending 리스너가 호출한다(단일 기록자, origin=SOLO).
// 멱등: 설문당 주문 1행 - PENDING 이면 재사용, 아니면 새 orderId 로 되살리고(재결제), 없으면 생성.
@Service
class PaymentOrderService(
    private val paymentOrderRepository: PaymentOrderRepository,
) {
    @Transactional
    fun issueOrder(
        surveyId: UUID,
        makerId: UUID,
        amount: Int,
    ): String {
        val order =
            paymentOrderRepository
                .findBySurveyId(surveyId)
                .map { existing ->
                    // 이전 주문이 결제 가능 상태(PENDING)가 아니면 새 orderId로 되살린다 (재결제)
                    if (existing.status != PaymentOrderStatus.PENDING) {
                        existing.renew("ord-${UUID.randomUUID()}", amount)
                    }
                    existing
                }.orElseGet {
                    paymentOrderRepository.save(
                        PaymentOrder.create(
                            surveyId = surveyId,
                            makerId = makerId,
                            orderId = "ord-${UUID.randomUUID()}",
                            amount = amount,
                            origin = PaymentOrderOrigin.SOLO,
                        ),
                    )
                }
        return order.tossOrderId
    }
}
