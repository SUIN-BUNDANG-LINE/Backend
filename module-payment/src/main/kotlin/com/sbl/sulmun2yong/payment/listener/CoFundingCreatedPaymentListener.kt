package com.sbl.sulmun2yong.payment.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.cofunding.dto.event.CoFundingCreatedEvent
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.dto.event.ProductType
import com.sbl.sulmun2yong.payment.entity.TossOrderEntity
import com.sbl.sulmun2yong.payment.repository.TossOrderRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

// ② co-funding-created 구독 - 결제 서비스의 주문 발급 리스너. toss_orders 는 결제만 쓴다(단일 기록자).
// 참여자마다 주문 1건(origin=CO_FUNDING) - orderId·amount 는 모금이 확정해 페이로드로 준 값이라 교차 읽기가 없다.
// 재전달 안전: PK(orderId) 선조회로 멱등 - PK 제약이 경합까지 받친다.
@Component
class CoFundingCreatedPaymentListener(
    private val objectMapper: ObjectMapper,
    private val tossOrderRepository: TossOrderRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(CoFundingCreatedPaymentListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.CO_FUNDING_CREATED],
        groupId = "payment-cofunding-created",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, CoFundingCreatedEvent::class.java)
        val boardId = UUID.fromString(event.boardId)

        val issuedCount =
            event.participants.count { participant -> issueIfAbsent(boardId, participant) }

        log.info(
            "모금 주문 발급 - fundingId={}, 참여자 {}명 중 {}건 신규(나머지는 재전달 멱등 스킵)",
            event.fundingId,
            event.participants.size,
            issuedCount,
        )
        ack.acknowledge()
    }

    // 이미 발급된 주문이면 아무것도 하지 않는다. 반환값 = 이번에 새로 발급했는지 여부
    private fun issueIfAbsent(
        boardId: UUID,
        participant: CoFundingCreatedEvent.Participant,
    ): Boolean {
        if (tossOrderRepository.findById(participant.orderId).isPresent) return false

        tossOrderRepository.save(
            TossOrderEntity.create(
                id = participant.orderId,
                productType = ProductType.DRAWING_BOARD,
                productId = boardId,
                payerId = UUID.fromString(participant.userId),
                amount = participant.amount,
            ),
        )
        return true
    }
}
