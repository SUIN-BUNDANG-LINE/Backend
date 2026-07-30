package com.sbl.sulmun2yong.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipant
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.entity.CoFundingStatus
import com.sbl.sulmun2yong.cofunding.exception.CoFundingNotFoundException
import com.sbl.sulmun2yong.cofunding.publisher.CoFundingEventPublisher
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandStatus
import com.sbl.sulmun2yong.payment.entity.PaymentCommandType
import com.sbl.sulmun2yong.payment.entity.PaymentOrder
import com.sbl.sulmun2yong.payment.repository.PaymentCommandRepository
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import com.sbl.sulmun2yong.survey.domain.SurveyStatus
import com.sbl.sulmun2yong.survey.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

// 결제 확정 전이 전달 - DB 만 만지는 짧은 트랜잭션 메서드 묶음.
// 어댑터(HTTP)는 여기 없다: "tx -> HTTP -> tx" 조립은 릴레이 & 핸들러가 한다.
@Service
class PaymentSettleService(
    private val paymentCommandRepository: PaymentCommandRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val surveyRepository: SurveyRepository,
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val coFundingEventPublisher: CoFundingEventPublisher,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(PaymentSettleService::class.java)

        // 이 시간 안의 커맨드는 success 핸들러가 처리 중일 수 있다 - 릴레이는 건드리지 않는다
        private const val RELAY_STALE_SECONDS = 60L

        private const val CANCEL_REASON = "공동 모금 무산 전액 환불"
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
        // 설문 활성화 - 결제 대기 중이었을 때만 연다 (이미 열렸으면 멱등 스킵)
        val participant = coFundingParticipantRepository.findByTossOrderId(command.aggregateId)
        if (participant == null) {
            settleSoloApproved(order)
        } else {
            settleCoFundingApproved(participant)
        }
    }

    // 단독 결제 - 결제 즉시 설문 활성화 (기존 동작, 결제 대기였을 때만 - 이미 열렸으면 멱등 스킵)
    private fun settleSoloApproved(order: PaymentOrder) {
        val survey = surveyRepository.findByIdAndIsDeletedFalse(order.surveyId).orElseThrow()
        if (survey.status == SurveyStatus.PENDING_PAYMENT) {
            surveyRepository.save(survey.start())
        }
    }

    // 공동 모금(D6) - 잠금 조회(FOR UPDATE)로 무산 CAS(tryFail)의 직렬화된 상태 검사가
    // "무산 확정 vs 결제 확정" 경합의 결승선. 잠금 없이 읽으면 검사 통과 직후 무산이 확정되는
    // 틈새에서 늦게 커밋된 SETTLED 를 환불 리스너가 못 보는 환불 누락 창이 열린다.
    // 설문 활성화는 여기 없다 - 전원 완료 장벽 CAS 승자(집계 리스너)의 몫
    private fun settleCoFundingApproved(participant: CoFundingParticipant) {
        val funding =
            coFundingRepository.findByIdForUpdate(participant.fundingId)
                ?: throw CoFundingNotFoundException()

        when (funding.status) {
            CoFundingStatus.FUNDING -> {
                participant.settle(LocalDateTime.now())
                coFundingEventPublisher.publishSettled(funding, participant)
            }

            CoFundingStatus.FAILED, CoFundingStatus.REFUNDED -> {
                // 늦은 결제 - SETTLED 기록 없이 즉시 전액 환불 커맨드 적재
                enqueueCancelCommand(participant.tossOrderId)
                log.warn("무산 후 늦은 결제 - 즉시 환불 적재: orderId={}", participant.tossOrderId)
            }

            CoFundingStatus.CONFIRMED -> {
                // 도달 불가(전원 SETTLED여야 CONFIRMED, 중복 settle은 커맨드 멱등 가드가 흡수) - 방어 로그만
                log.warn("개설 확정 모금에 결제 확정 도착(무시): orderId={}", participant.tossOrderId)
            }
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

        val order = paymentOrderRepository.findByTossOrderId(command.aggregateId).orElseThrow()
        order.markFailed()
        log.warn("결제 거절 확정 - orderId={}, code={}", command.aggregateId, code)
        // 공등 모금이면 설문 복귀 없음 - 모금은 FUNDING 유지, 거절 참여자는 기한 만료 무산 경로.
        // 단독 결제만 작성자가 재시작(재결제)할 수 있게 설문을 되돌린다
        // null이면 단독결제라는 의미이다
        if (coFundingParticipantRepository.findByTossOrderId(command.aggregateId) == null) {
            val survey = surveyRepository.findByIdAndIsDeletedFalse(order.surveyId).orElseThrow()
            if (survey.status == SurveyStatus.PENDING_PAYMENT) {
                surveyRepository.save(survey.revertToNotStarted())
            }
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

    // CANCEL 발송 준비 - 취소는 승인 때 받은 paymentKey 로 부른다 (장부에서 해석).
    // 반환: 토스 취소 API 에 쓸 paymentKey.
    // null = 주문이 없거나 confirm 전이라 키 미기록. DONE 주문 없이 CANCEL 은 적재될 수
    // 없으므로 데이터 이상 신호다 - 호출자(릴레이)는 recordRetry 로 다음 주기에 재확인한다.
    @Transactional(readOnly = true)
    fun findPaymentKeyByOrderId(orderId: String): String? = paymentOrderRepository.findByTossOrderId(orderId).orElse(null)?.paymentKey

    // CANCEL 전이 tx - 장부 CANCELED + 참여자 REFUNDED. 커맨드 확정 도장은 tx 가 맨 마지막에 찍는다.
    // 반환: 판정 tx(settleCancelJudged)에 넘길 모금 ID.
    // UUID = 참여자 행을 찾음. 참여자가 이미 REFUNDED(재실행)여도 반환한다
    //        직전 실행이 판정 직전에 죽었을 수 있으므로 판정은 다시 시도돼야 한다.
    // null = (a) 커맨드가 이미 CONFIRMED: 전이/판정까지 끝난 건의 중복 처리 - 전부 스킵
    //        (b) 주문에 연결된 참여자 없음: co-funding 주문이 아닌 이상 데이터 - 판정 대상 없ㅇ므
    // 호출자는 null 이면 판정 CAS 를 건너뛴다.
    @Transactional
    fun settleCancelTransition(commandId: UUID): UUID? {
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
        if (command.status == PaymentCommandStatus.CONFIRMED) return null

        val order = paymentOrderRepository.findByTossOrderId(command.aggregateId).orElseThrow()
        order.markCanceled()

        val participant = coFundingParticipantRepository.findByTossOrderId(command.aggregateId)
        // 늦은 결제(D6)의 취소는 참여자가 REGISTERED 인 채로 온다 - SETTLE 일 때만 환불 전이
        if (participant?.status == CoFundingParticipantStatus.SETTLED) {
            participant.markRefunded()
        }
        log.info("환불 확정 - orderId={}", command.aggregateId)
        return participant?.fundingId
    }

    // CANCEL 판정 tx - tryMarkRefunded 의 반환 값(UPDATE 가 갱신한 행 수)을 소비한다:
    // 1 = 마지막 잔여 환불을 치워 FAILED -> REFUNDED 를 실제로 만든 유일한 승자.
    //     이 후처리는 참여자 수 x 재시도만큼 반복 실행되지만 1을 받는 트랜잭션은 모금당
    //     정확히 하나다 - 승자 전용 후속(로그, 훗날 완료 알림)은 이 if 안이 자리다.
    // 0 = 아직 SETTLED 잔여가 있거나(내가 마지막이 아님), 이미 REFUNDED(중복/재실행) - no-op.
    // 어느 쪽이든 마지막에 커맨드 확정 도장을 찍어 재클레임 루프에서 내보낸다.
    @Transactional
    fun settleCancelJudged(
        commandId: UUID,
        fundingId: UUID?,
    ) {
        if (fundingId != null &&
            coFundingRepository.tryMarkRefunded(fundingId, LocalDateTime.now()) == 1
        ) {
            log.info("공동 모금 전원 환불 수렴 - fundingId={}", fundingId)
        }
        val command = paymentCommandRepository.findById(commandId).orElseThrow()
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

    // CANCEL 적재 - 이중 환불의 최종 방어는 UNIQUE(aggregate_id, command_type).
    // 사전 exists 검사 흔한 중복(settle 재시도/환불 리스너와의 경합)을 조용히 흡수하고,
    // 검사 틈새의 동시 삽입은 UNIQUE 위반 -> 호출 tx 롤백 -> 재시도의 exists 에서 수렴한다
    @Transactional
    fun enqueueCancelCommand(tossOrderId: String) {
        if (paymentCommandRepository.existsByAggregateIdAndCommandType(
                tossOrderId,
                PaymentCommandType.CANCEL,
            )
        ) {
            return
        }
        paymentCommandRepository.save(
            PaymentCommand.create(
                commandType = PaymentCommandType.CANCEL,
                aggregateId = tossOrderId,
                requestPayload =
                    objectMapper.writeValueAsString(
                        TossCancelRequest(
                            CANCEL_REASON,
                        ),
                    ),
            ),
        )
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
