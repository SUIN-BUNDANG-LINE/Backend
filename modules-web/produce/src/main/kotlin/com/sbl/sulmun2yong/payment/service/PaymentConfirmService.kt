package com.sbl.sulmun2yong.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.adapter.TossConfirmResult
import com.sbl.sulmun2yong.payment.adapter.TossPaymentsAdapter
import com.sbl.sulmun2yong.payment.dto.TossConfirmRequest
import com.sbl.sulmun2yong.payment.entity.PaymentOrderStatus
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// successUrl 처리의 두뇌- 검증 -> 커맨드 적재(먼저 커밋!) -> 동기 confirm -> settle.
// 트랜잭션 구조는 릴레이와 동일: 적재 tx-> HTTP(tx 밖)-> settle tx.
@Service
class PaymentConfirmService(
    private val paymentOrderRepository: PaymentOrderRepository,
    private val paymentSettleService: PaymentSettleService,
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
        // 1. 금액 위변조 검증 - successUrl 쿼리를 믿지 않고 장부와 대조
        val order =
            paymentOrderRepository.findByTossOrderId(orderId).orElse(null)
                ?: return ConfirmOutcome.FAILED
        if (order.amount != amount) {
            log.warn("금액 위변조 의심 - orderId={}, 장부={}, 쿼리={}", orderId, order.amount, amount)
            return ConfirmOutcome.FAILED
        }

        // 이미 확정된 주문의 의도치않은 재요청(새로고침 등) - confirm 없이 바로 결과로 (멱등)
        if (order.status == PaymentOrderStatus.DONE) return ConfirmOutcome.DONE
        if (order.status != PaymentOrderStatus.PENDING) return ConfirmOutcome.FAILED

        // 2. 커맨드 적재 - 먼제 커밋해야 직후 서버가 죽어도 릴레이가 이어받는다 (Command Outbox 핵심)
        val command =
            paymentSettleService.enqueueConfirmCommand(
                orderId = orderId,
                requestPayload =
                    objectMapper.writeValueAsString(
                        TossConfirmRequest(
                            paymentKey = paymentKey,
                            orderId = orderId,
                            amount = amount,
                        ),
                    ),
            )

        // 3. 동기 confirm (tx 밖) -> settle
        return when (val result = tossPaymentsAdapter.confirm(paymentKey, orderId, amount)) {
            is TossConfirmResult.Approved -> {
                paymentSettleService.settleApproved(command.id, result.paymentKey)
                ConfirmOutcome.DONE
            }

            is TossConfirmResult.Rejected -> {
                paymentSettleService.settleRejected(command.id, result.code)
                ConfirmOutcome.FAILED
            }

            TossConfirmResult.Unknown -> {
                // SENT로 남겨 릴레이 수거 대상으로
                paymentSettleService.markSent(command.id)
                ConfirmOutcome.PROCESSING
            }
        }
    }

}
