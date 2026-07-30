package com.sbl.sulmun2yong.cofunding.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingFailedConsumedEvent
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.payment.dto.TossCancelRequest
import com.sbl.sulmun2yong.payment.entity.PaymentCommand
import com.sbl.sulmun2yong.payment.entity.PaymentCommandType
import com.sbl.sulmun2yong.payment.repository.PaymentCommandRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class CoFundingRefundEventListener(
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
    private val paymentCommandRepository: PaymentCommandRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingRefundEventListener::class.java)
        private const val CANCEL_REASON = "공동 모금 무산 전액 환불"
    }

    @EventListener
    fun handle(event: CoFundingFailedConsumedEvent) {
        val fundingId = UUID.fromString(event.fundingId)
        // 이벤트의 settledOrderIds 스냅샷이 아니라 DB 재조회가 진실 - 늦은 확정(D6 경계)을 놓치지 않는다
        val settled =
            coFundingParticipantRepository.findAllByFundingIdAndStatus(
                fundingId,
                CoFundingParticipantStatus.SETTLED,
            )

        if (settled.isEmpty()) {
            // 결제자 0명 무산 - CANCEL 이 없어 릴레이 후처리가 발동할 수 없으므로 직접 종착 (D5 예외)
            if (coFundingRepository.tryMarkRefunded(fundingId, LocalDateTime.now()) == 1) {
                log.info("결제자 0명 무산 - 즉시 종착: fundingId={}", event.fundingId)
            }
            return
        }

        val enqueued =
            settled.count { participant ->
                if (paymentCommandRepository.existsByAggregateIdAndCommandType(
                        participant.tossOrderId,
                        PaymentCommandType.CANCEL,
                    )
                ) {
                    false
                } else {
                    paymentCommandRepository.save(
                        PaymentCommand.create(
                            commandType = PaymentCommandType.CANCEL,
                            aggregateId = participant.tossOrderId,
                            requestPayload =
                                objectMapper.writeValueAsString(
                                    TossCancelRequest(
                                        CANCEL_REASON,
                                    ),
                                ),
                        ),
                    )
                    true
                }
            }

        log.info("환불 팬아웃 - fundingId={}, 대상 {}건 중 {}건 적재", event.fundingId, settled.size, enqueued)
    }
}
