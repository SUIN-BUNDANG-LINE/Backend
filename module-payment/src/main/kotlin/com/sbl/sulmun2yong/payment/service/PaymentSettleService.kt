package com.sbl.sulmun2yong.payment.service

import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandStatus
import com.sbl.sulmun2yong.payment.entity.PaymentCommandType
import com.sbl.sulmun2yong.payment.publisher.PaymentEventPublisher
import com.sbl.sulmun2yong.payment.repository.PaymentCommandRepository
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

// 결제 확정 전이 전달 - DB 만 만지는 짧은 트랜잭션 메서드 묶음.
// 어댑터(HTTP)는 여기 없다: "tx -> HTTP -> tx" 조립은 릴레이 & 핸들러가 한다.
// 단일 기록자: 결제는 payment_* 만 쓴다 - participants·co_fundings·surveys 전이는
// ④settled·⑦refunded·failed 사실 이벤트를 구독하는 모금·설문 리스너 몫.
@Service
class PaymentSettleService(
    private val paymentCommandRepository: PaymentCommandRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val paymentEventPublisher: PaymentEventPublisher,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentSettleService::class.java)

        // 이 시간 안의 커맨드는 success 핸들러가 처리 중일 수 있다 - 릴레이는 건드리지 않는다
        private const val RELAY_STALE_SECONDS = 60L
    }

    // 릴레이 클레임 - 오래 미확정(PENDING & SENT)인 심부름을 SKIP LOCKED 로 집는다
    @Transactional
    fun claimForRelay(limit: Int): List<PaymentCommand> =
        paymentCommandRepository.findRetryableForUpdateSkipLocked(
            listOf(PaymentCommandStatus.PENDING, PaymentCommandStatus.SENT),
            Instant.now().minusSeconds(RELAY_STALE_SECONDS),
            PageRequest.of(0, limit),
        )

    // 발송 직전 기록 - "보냈다" 를 남겨야 응답 유실 시에도 SENT로 추적된다
    @Transactional
    fun markSent(commandId: UUID) {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        command.markSent()
    }

    // 승인 확정 - 커맨드 CONFIRMED + 장부 DONE. 핸들러 & 릴레이가 경쟁해도 멱등(이미 확정이면 no-op).
    @Transactional
    fun settleApproved(
        commandId: UUID,
        paymentKey: String,
    ) {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        if (command.status == PaymentCommandStatus.CONFIRMED) {
            log.debug("이미 확정된 커맨드(멱등 스킵): {}", commandId)
            return
        }
        command.markConfirmed()

        val order = paymentOrderRepository.findByTossOrderId(command.aggregateId).orElseThrow()
        order.markDone(paymentKey)
        log.info("결제 확정 - orderId={}, paymentKey={}", command.aggregateId, paymentKey)
        // confirm 성공 사실 발행(단독·모금 불문) - 이후는 전부 구독자 몫(단일 기록자):
        // 단독 설문 활성화 = 설문 리스너, 모금 SETTLED 전이·장벽 CAS·늦은 결제 판정 = 모금 ④ 리스너.
        paymentEventPublisher.publishSettled(order)
    }

    // 명시적 거절 확정 - 커맨드 & 장부 FAILED
    @Transactional
    fun settleRejected(
        commandId: UUID,
        code: String,
    ) {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        if (command.status == PaymentCommandStatus.FAILED) return
        command.markFailed()

        val order = paymentOrderRepository.findByTossOrderId(command.aggregateId).orElseThrow()
        order.markFailed()
        log.warn("결제 거절 확정 - orderId={}, code={}", command.aggregateId, code)
        // confirm 거절 사실 발행 - 단독 설문 복귀(재결제 가능화)는 설문 리스너 몫(단일 기록자).
        // 모금 거절은 설문 복귀 없음(FUNDING 유지·기한 만료 무산 경로) - 리스너가 모금 건을 스킵한다.
        paymentEventPublisher.publishFailed(order)
    }

    // 미확정 대시도 카운트 - MAX 소진 시 FAILED 전환
    @Transactional
    fun recordRetry(commandId: UUID) {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        if (command.incrementRetry()) {
            log.error("결제 확정 재시도 소진 - orderId={} 수동 확인 필요", command.aggregateId)
        }
    }

    // CANCEL 발송 준비 - 취소는 승인 때 받은 paymentKey 로 부른다 (장부에서 해석).
    // 반환: 토스 취소 API 에 쓸 paymentKey.
    // null = 주문이 없거나 confirm 전이라 키 미기록. DONE 주문 없이 CANCEL 은 적재될 수
    // 없으므로 데이터 이상 신호다 - 호출자(릴레이)는 recordRetry 로 다음 주기에 재확인한다.
    @Transactional(readOnly = true)
    fun findPaymentKeyByOrderId(orderId: String): String? = paymentOrderRepository.findByTossOrderId(orderId).orElse(null)?.paymentKey

    // CANCEL 승인 확정 - 장부 CANCELED + ⑦ 환불 완료 사실 발행 + 커맨드 확정 도장, 한 tx.
    // 참여자 REFUNDED 전이·FAILED→REFUNDED 수렴 CAS 는 모금 ⑦ 리스너 몫(단일 기록자)이라
    // 예전의 전이 tx/판정 tx 분리가 필요 없어졌다 - 판정이 모금 로컬 tx 로 갔다.
    @Transactional
    fun settleCancelled(commandId: UUID) {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        if (command.status == PaymentCommandStatus.CONFIRMED) {
            log.debug("이미 확정된 취소 커맨드(멱등 스킵): {}", commandId)
            return
        }

        val order = paymentOrderRepository.findByTossOrderId(command.aggregateId).orElseThrow()
        order.markCanceled()
        log.info("환불 확정 - orderId={}", command.aggregateId)
        paymentEventPublisher.publishRefunded(order)
        command.markConfirmed()
    }

    // 취소 명시적 거절 - 재시도 무의미. ALREADY_CANCELED 는 어댑터가 성공으로 흡수하므로
    // 여기 도달하면 정산 이슈 등 진짜 이상 상황 - 돈이 묶인 상태라 수동 개입 필요
    @Transactional
    fun settleCancelRejected(
        commandId: UUID,
        code: String,
    ) {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        if (command.status == PaymentCommandStatus.FAILED) return
        command.markFailed()
        log.error("환불 거절 - orderId={}, code={} 수동 확인 필요", command.aggregateId, code)
    }

    // 적재 전용 짧은 트랜잭션 - 이 커밋이 "무슨 일이 있어도 confirm은 완수한다"는 보증서다
    @Transactional
    fun enqueueConfirmCommand(
        orderId: String,
        requestPayload: String,
    ): PaymentCommand =
        paymentCommandRepository.save(
            PaymentCommand.create(
                commandType = PaymentCommandType.CONFIRM,
                aggregateId = orderId,
                requestPayload = requestPayload,
            ),
        )
}
