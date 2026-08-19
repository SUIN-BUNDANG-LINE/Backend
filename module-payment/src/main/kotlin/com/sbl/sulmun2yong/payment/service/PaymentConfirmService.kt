package com.sbl.sulmun2yong.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.adapter.TossConfirmResult
import com.sbl.sulmun2yong.payment.adapter.TossPaymentsAdapter
import com.sbl.sulmun2yong.payment.dto.TossConfirmRequest
import com.sbl.sulmun2yong.payment.entity.TossOrderStatus
import com.sbl.sulmun2yong.payment.repository.TossOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentConfirmService(
    private val tossOrderRepository: TossOrderRepository,
    private val tossApiCallOutboxService: TossApiCallOutboxService,
    private val tossOrderService: TossOrderService,
    private val tossPaymentsAdapter: TossPaymentsAdapter,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentConfirmService::class.java)
    }

    fun handleSuccess(
        paymentKey: String,
        orderId: String,
        amount: Int,
    ): ConfirmOutcome {
        // 1. 금액 위변조 검증 - amount 와 TossOrderEntity 의 amount 가 일치하지 않으면 토스 API 호출 없이 빠른 실패
        val order =
            tossOrderRepository.findById(orderId).orElse(null)
                ?: return ConfirmOutcome.FAILED
        if (order.amount != amount) {
            log.warn("금액 위변조 의심 - orderId={}, 장부={}, 쿼리={}", orderId, order.amount, amount)
            return ConfirmOutcome.FAILED
        }

        // 2. 확정된 주문의 재요청 차단
        if (order.status == TossOrderStatus.SUCCEEDED) return ConfirmOutcome.SUCCEEDED
        if (order.status == TossOrderStatus.CANCELED) return ConfirmOutcome.FAILED

        // 3. 커맨드 적재 - 먼제 커밋해야 직후 서버가 죽어도 릴레이가 이어받는다 (Command Outbox 핵심)
        val tossApiCallOutbox =
            tossApiCallOutboxService.commitConfirmIntent(
                orderId = orderId,
                paymentKey = paymentKey,
                requestPayload =
                    objectMapper.writeValueAsString(
                        TossConfirmRequest(
                            paymentKey = paymentKey,
                            orderId = orderId,
                            amount = amount,
                        ),
                    ),
            )

        // 4. 동기적으로 tx 밖에서 toss confirm API 호출 -> 결과에따라 상태 전이
        return when (val result = tossPaymentsAdapter.confirm(paymentKey, orderId, amount)) {
            is TossConfirmResult.Approved -> {
                tossOrderService.settleApproved(tossApiCallOutbox.id, result.paymentKey)
                ConfirmOutcome.SUCCEEDED
            }

            is TossConfirmResult.Rejected -> {
                tossOrderService.settleRejected(tossApiCallOutbox.id, result.code)
                ConfirmOutcome.FAILED
            }

            // 명시적 실패가 아니면 미확정으로 두어야한다. 실패인가 성공인가? 구분할 수 없기 때문이다.
            // 커맨드는 PENDING 그대로 - 60초 뒤 릴레이가 재클레임해 조회로 수렴시킨다.
            TossConfirmResult.Unknown -> {
                ConfirmOutcome.PROCESSING
            }
        }
    }

}
