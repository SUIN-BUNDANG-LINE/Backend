package com.sbl.sulmun2yong.payment.relay

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.adapter.TossConfirmResult
import com.sbl.sulmun2yong.payment.adapter.TossPaymentsAdapter
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.dto.TossConfirmRequest
import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandType
import com.sbl.sulmun2yong.payment.service.PaymentSettleService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

// Command Outbox 릴레이 - success 핸들러가 못 끝낸 심부름(PENDING & SENT)을 수거해 끝까지 확정한다.
// 처리 순서가 "짧은 트랜잭션 -> HTTP -> 짧은 트랜잭션" 인 이유:
// DB 커넥션(풀 ~10개)은 @Transactional 이 끝날 때까지 반납되지 않는데, 토스 confirm은 수 초씩 걸린다.
// 트랜잭션 안에서 토스를 기다리면 그 수 초 동안 커넥션 하나가 통째로 묶이고,
// 결제가 몰리면 풀이 말라 결제와 무관한 요청까지 전부 멈춘다.
// 그래서 DB는 도장 찍을 때만 잠깐 쓰고(markSent / settle*), 토스 대기는 트랜잭션 밖에서 한다.
// 이 클래스 메서드에 @Transactional 이 없는 것은 실수가 아니라 이 설계의 핵심이다.
@Component
class PaymentCommandRelay(
    private val paymentSettleService: PaymentSettleService,
    private val tossPaymentsAdapter: TossPaymentsAdapter,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(PaymentCommandRelay::class.java)
        private const val BATCH_SIZE = 10
    }

    @Scheduled(fixedDelay = 10_000)
    fun relayPendingCommands() {
        paymentSettleService.claimForRelay(BATCH_SIZE).forEach { command ->
            // 한 건이 터져도 배치의 나모지는 게속 - 실패 건은 다음 주기가 다시 집는다
            runCatching { process(command) }
                .onFailure { log.error("커맨드 처리 실패 - 다음 주기 재시도, id={}", command.id, it) }
        }
    }

    private fun process(command: PaymentCommand) {
        // tx: 보냈다 기록
        paymentSettleService.markSent(command.id)

        when (command.commandType) {
            PaymentCommandType.CONFIRM -> processConfirm(command)
            PaymentCommandType.CANCEL -> processCancel(command)
        }
    }

    private fun processConfirm(command: PaymentCommand) {
        val payload = objectMapper.readValue(command.requestPayload, TossConfirmRequest::class.java)

        // HTTP (tx 밖) -> 삼분법 소비
        when (
            val result =
                tossPaymentsAdapter.confirm(payload.paymentKey, payload.orderId, payload.amount)
        ) {
            is TossConfirmResult.Approved -> {
                paymentSettleService.settleApproved(
                    command.id,
                    result.paymentKey,
                )
            }

            is TossConfirmResult.Rejected -> {
                paymentSettleService.settleRejected(
                    command.id,
                    result.code,
                )
            }

            TossConfirmResult.Unknown -> {
                resolveUnknown(command)
            }

        }
    }

    private fun processCancel(command: PaymentCommand) {
        val payload = objectMapper.readValue(command.requestPayload, TossCancelRequest::class.java)
        val paymentKey = paymentSettleService.findPaymentKeyByOrderId(command.aggregateId)
        if (paymentKey == null) {
            // DONE 주문 없이 CANCEL 은 적재될 수 없다 - 데이터 이상, 다음 주기 재확인
            log.error("취소 대상 주문의 paymentKey 없음 - orderId={}", command.aggregateId)
            paymentSettleService.recordRetry(command.id)
            return
        }

        when (
            val result =
                tossPaymentsAdapter.cancel(paymentKey, command.aggregateId, payload.cancelReason)
        ) {
            is TossConfirmResult.Approved -> {
                completeCancel(command)
            }

            is TossConfirmResult.Rejected -> {
                paymentSettleService.settleCancelRejected(
                    command.id,
                    result.code,
                )
            }

            TossConfirmResult.Unknown -> {
                resolveCancelUnknown(command)
            }
        }
    }

    // CANCEL 승인 확정 단일 tx - 장부 CANCELED + ⑦ 발행 + 커맨드 도장이 한 커밋.
    // 참여자 REFUNDED·수렴 CAS 판정은 모금 ⑦ 리스너 몫이라 전이/판정 2-tx 분리가 사라졌다.
    // tx 전에 죽으면 SENT 로 남아 재클레임이 이어 간다.
    private fun completeCancel(command: PaymentCommand) {
        paymentSettleService.settleCancelled(command.id)
    }

    // 미확정 - 조회("그 결제 어떻게 됐어?")로 수렴을 시도하고, 그래도 모르면 재시도 카운트만 올린다
    private fun resolveUnknown(command: PaymentCommand) {
        val payment = tossPaymentsAdapter.getOrder(command.aggregateId)
        when (payment?.status) {
            "DONE" -> {
                paymentSettleService.settleApproved(command.id, payment.paymentKey)
            }

            "ABORTED", "EXPIRED", "CANCELED" -> {
                paymentSettleService.settleRejected(
                    command.id,
                    payment.status,
                )
            }

            // null 포함 - 다음 주기에 다시
            else -> {
                paymentSettleService.recordRetry(command.id)
            }
        }
    }

    // CANCEL 미확정 - 실제 상태가 CANCELED 면 취소 성공으로 마감, 아니면 다음 주기 재시도
    private fun resolveCancelUnknown(command: PaymentCommand) {
        val payment = tossPaymentsAdapter.getOrder(command.aggregateId)
        when (payment?.status) {
            "CANCELED" -> completeCancel(command)
            else -> paymentSettleService.recordRetry(command.id)
        }
    }
}
