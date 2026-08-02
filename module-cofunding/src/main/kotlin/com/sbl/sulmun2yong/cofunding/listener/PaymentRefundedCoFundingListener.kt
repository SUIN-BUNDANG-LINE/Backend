package com.sbl.sulmun2yong.cofunding.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.entity.CoFundingParticipantStatus
import com.sbl.sulmun2yong.cofunding.repository.CoFundingParticipantRepository
import com.sbl.sulmun2yong.cofunding.repository.CoFundingRepository
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.event.PaymentRefundedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

// ⑦ payment-refunded 구독 - 모금 서비스의 환불 수렴 리스너. participants·co_fundings 는 모금만 쓴다.
// 릴레이 CANCEL 후처리가 하던 참여자 REFUNDED 전이·FAILED→REFUNDED 수렴 CAS 를 대체한다.
// 수렴 CAS(tryMarkRefunded)는 SETTLED 잔여 0명 시점에만 1을 반환 - 참여자 수 x 재시도만큼
// 반복돼도 승자는 모금당 정확히 하나다(승자 전용 후속 - 로그, 훗날 완료 알림 - 는 그 안이 자리다).
// 재전달 안전: SETTLED 가드·CAS no-op 이 곧 멱등이다.
@Component
class PaymentRefundedCoFundingListener(
    private val objectMapper: ObjectMapper,
    private val coFundingRepository: CoFundingRepository,
    private val coFundingParticipantRepository: CoFundingParticipantRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentRefundedCoFundingListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.PAYMENT_REFUNDED],
        groupId = "cofunding-payment-refunded",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, PaymentRefundedEvent::class.java)
        val participant = coFundingParticipantRepository.findByTossOrderId(event.orderId)

        if (participant == null) {
            log.debug("모금 주문 아님 - 환불 수렴 스킵: orderId={}", event.orderId)
            ack.acknowledge()
            return
        }

        // 늦은 결제의 취소는 참여자가 REGISTERED 인 채로 온다 - SETTLED 일 때만 환불 전이
        if (participant.status == CoFundingParticipantStatus.SETTLED) {
            participant.markRefunded()
        }

        if (coFundingRepository.tryMarkRefunded(participant.fundingId, LocalDateTime.now()) == 1) {
            log.info("공동 모금 전원 환불 수렴 - fundingId={}", participant.fundingId)
        }
        ack.acknowledge()
    }
}
