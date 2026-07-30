package com.sbl.sulmun2yong.payment.service

import com.sbl.sulmun2yong.payment.entity.PaymentOrderStatus
import com.sbl.sulmun2yong.payment.entity.PaymentWebhookInbox
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import com.sbl.sulmun2yong.payment.repository.PaymentWebhookInboxRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 취소 웹훅의 멱등 수신 - 전화 기록 장부(inbox)에 적고, 취소를 결제 장부에 반영한다.
// HTTP 호출이 없어 전체가 하나의 짧은 트랜잭션이어도 안전하다.
@Service
class PaymentWebhookService(
    private val paymentWebhookInboxRepository: PaymentWebhookInboxRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaymentWebhookService::class.java)
    }

    @Transactional
    fun handle(
        webhookId: String,
        eventType: String,
        orderId: String,
        paymentKey: String,
        status: String,
        rawPayload: String,
    ) {
        // 1차 방어: 조회로 조기 스킵. 최종 방어는 webhook_id UNIQUE (동시 수신 대비)
        if (paymentWebhookInboxRepository.existsByWebhookId(webhookId)) {
            log.info("중복 웹훅 스킵(멱둥) - webhookId={}", webhookId)
            return
        }

        val inbox =
            paymentWebhookInboxRepository.save(
                PaymentWebhookInbox.create(
                    webhookId = webhookId,
                    orderId = orderId,
                    eventType = eventType,
                    payload = rawPayload,
                ),
            )

        // MVP 는 취소 반영만 - 환불 & 설문 후속 처리는 B단계(부분취소 사가)
        if (status == "CANCELED" || status == "PARTIAL_CANCELED") {
            paymentOrderRepository.findByTossOrderId(orderId).ifPresent { order ->
                if (order.status == PaymentOrderStatus.DONE) {
                    order.markCanceled()
                    log.warn("결제 취소 웹훅 반영 - orderId={}, status={}", orderId, status)
                }
            }
        }

        inbox.markProcessed()
    }
}
