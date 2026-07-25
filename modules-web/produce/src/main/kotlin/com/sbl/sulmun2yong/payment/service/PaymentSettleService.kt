package com.sbl.sulmun2yong.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandStatus
import com.sbl.sulmun2yong.payment.entity.PaymentCommandType
import com.sbl.sulmun2yong.payment.repository.PaymentCommandRepository
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

// 결제 확정 전이 전달 - DB 만 만지는 짧은 트랜잭션 메서드 묶음.
// 어댑터(HTTP)는 여기 없다: "tx -> HTTP -> tx" 조립은 릴레이 & 핸들러가 한다.
@Service
class PaymentSettleService(
    private val paymentCommandRepository: PaymentCommandRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val surveyRepository: SurveyRepository,
    private val objectMapper: ObjectMapper,
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

        val order = paymentOrderRepository.findByOrderId(command.aggregateId).orElseThrow()
        order.markDone(paymentKey)
        log.info("결제 확정 - orderId={}, paymentKey={}", command.aggregateId, paymentKey)
        // 설문 활성화 - 결제 대기 중이었을 때만 연다 (이미 열렸으면 멱등 스킵)
        val survey =
            surveyRepository.findByIdAndIsDeletedFalse(order.surveyId).orElseThrow()
        if (survey.status == SurveyStatus.PENDING_PAYMENT) {
            surveyRepository.save(survey.start())
        }
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

        val order = paymentOrderRepository.findByOrderId(command.aggregateId).orElseThrow()
        order.markFailed()
        log.warn("결제 거절 확정 - orderId={}, code={}", command.aggregateId, code)
        // 설문 복귀 - 작성자가 다시 시작(재결제)할 수 있게 되돌린다
        val survey = surveyRepository.findByIdAndIsDeletedFalse(order.surveyId).orElseThrow()
        if (survey.status == SurveyStatus.PENDING_PAYMENT) {
            surveyRepository.save(survey.revertToNotStarted())
        }
    }

    // 미확정 대시도 카운트 - MAX 소진 시 FAILED 전환
    @Transactional
    fun recordRetry(commandId: UUID) {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        if (command.incrementRetry()) {
            log.error("결제 확정 재시도 소진 - orderId={} 수동 확인 필요", command.aggregateId)
        }
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
