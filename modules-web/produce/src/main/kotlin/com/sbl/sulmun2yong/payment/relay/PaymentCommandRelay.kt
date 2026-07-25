package com.sbl.sulmun2yong.payment.relay

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.adapter.TossConfirmResult
import com.sbl.sulmun2yong.payment.adapter.TossPaymentsAdapter
import com.sbl.sulmun2yong.payment.dto.TossConfirmRequest
import com.sbl.sulmun2yong.payment.entity.PaymentCommand
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
        val payload = objectMapper.readValue(command.requestPayload, TossConfirmRequest::class.java)

        // tx: 보냈다 기록
        paymentSettleService.markSent(command.id)

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
}
