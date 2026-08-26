package com.sbl.sulmun2yong.payment.relay

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.adapter.TossConfirmResult
import com.sbl.sulmun2yong.payment.adapter.TossPaymentsAdapter
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.dto.TossConfirmRequest
import com.sbl.sulmun2yong.payment.entity.TossApiCallOutboxEntity
import com.sbl.sulmun2yong.payment.entity.TossApiCallType
import com.sbl.sulmun2yong.payment.service.TossApiCallOutboxService
import com.sbl.sulmun2yong.payment.service.TossOrderService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class TossApiCallOutboxRelay(
    private val tossApiCallOutboxService: TossApiCallOutboxService,
    private val tossOrderService: TossOrderService,
    private val tossPaymentsAdapter: TossPaymentsAdapter,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(TossApiCallOutboxRelay::class.java)
        private const val BATCH_SIZE = 10
        private const val STALE_SECONDS = 60L
    }

    @Scheduled(fixedDelay = 10_000)
    fun relayPendingCalls() {
        tossApiCallOutboxService.claimPendingForRelay(BATCH_SIZE, STALE_SECONDS).forEach { call ->
            runCatching { process(call) }.onFailure { e ->
                log.error("호출 처리 실패 - 재시도 카운터 소모, id={}", call.id, e)
                runCatching { tossApiCallOutboxService.incrementRetry(call.id) }
                    .onFailure { log.error("재시도 카운터 증가 실패 - id={}", call.id, it) }
            }
        }
    }

    private fun process(call: TossApiCallOutboxEntity) {
        when (call.callType) {
            TossApiCallType.CONFIRM -> processConfirm(call)
            TossApiCallType.CANCEL -> processCancel(call)
        }
    }

    private fun processConfirm(call: TossApiCallOutboxEntity) {
        val payload = objectMapper.readValue(call.requestPayload, TossConfirmRequest::class.java)

        when (
            val result =
                tossPaymentsAdapter.confirm(payload.paymentKey, payload.orderId, payload.amount)
        ) {
            is TossConfirmResult.Approved -> {
                tossOrderService.settleApproved(
                    call.id,
                    result.paymentKey,
                )
            }

            is TossConfirmResult.Rejected -> {
                tossOrderService.settleRejected(
                    call.id,
                    result.code,
                )
            }

            TossConfirmResult.Unknown -> {
                resolveUnknown(call)
            }

        }
    }

    private fun processCancel(call: TossApiCallOutboxEntity) {
        val payload = objectMapper.readValue(call.requestPayload, TossCancelRequest::class.java)
        val paymentKey = tossOrderService.findPaymentKeyByOrderId(call.tossOrderId)
        if (paymentKey == null) {
            log.error("취소 대상 주문의 paymentKey 없음 - orderId={}", call.tossOrderId)
            tossApiCallOutboxService.incrementRetry(call.id)
            return
        }

        when (
            val result =
                tossPaymentsAdapter.cancel(
                    paymentKey,
                    call.tossOrderId,
                    payload.cancelReason,
                    call.retry,
                )
        ) {
            is TossConfirmResult.Approved -> {
                completeCancel(call)
            }

            is TossConfirmResult.Rejected -> {
                tossApiCallOutboxService.markFailed(call.id)?.let {
                    log.error("환불 거절 - orderId={}, code={} 수동 확인 필요", it.tossOrderId, result.code)
                }
            }

            TossConfirmResult.Unknown -> {
                resolveCancelUnknown(call)
            }
        }
    }

    private fun completeCancel(call: TossApiCallOutboxEntity) {
        tossOrderService.settleCancelled(call.id)
    }

    private fun resolveUnknown(call: TossApiCallOutboxEntity) {
        val payment = tossPaymentsAdapter.getOrder(call.tossOrderId)
        when (payment?.status) {
            "DONE" -> {
                tossOrderService.settleApproved(call.id, payment.paymentKey)
            }

            "ABORTED", "EXPIRED", "CANCELED" -> {
                tossOrderService.settleRejected(
                    call.id,
                    payment.status,
                )
            }

            else -> {
                tossApiCallOutboxService.incrementRetry(call.id)
            }
        }
    }

    private fun resolveCancelUnknown(call: TossApiCallOutboxEntity) {
        val payment = tossPaymentsAdapter.getOrder(call.tossOrderId)
        when (payment?.status) {
            "CANCELED" -> completeCancel(call)
            else -> tossApiCallOutboxService.incrementRetry(call.id)
        }
    }
}
