package com.sbl.sulmun2yong.payment.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.global.kafka.config.KafkaTopics
import com.sbl.sulmun2yong.payment.service.PaymentOrderService
import com.sbl.sulmun2yong.survey.dto.event.SurveyPaymentPendingEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// survey-payment-pending 구독 - 단독(비모금) 설문의 주문 발급 리스너. payment_orders 는 결제만 쓴다.
// 내부 동기 API(POST /internal/payments/orders)를 대체한다 - 설문 개시 tx 의 Outbox 발행으로 수렴.
// 재전달·재호출 안전: 발급이 설문당 1행 멱등(PENDING 재사용·비활성 주문은 새 orderId 로 renew).
@Component
class SurveyPaymentPendingListener(
    private val objectMapper: ObjectMapper,
    private val paymentOrderService: PaymentOrderService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(SurveyPaymentPendingListener::class.java)
    }

    @KafkaListener(
        topics = [KafkaTopics.SURVEY_PAYMENT_PENDING],
        groupId = "payment-survey-payment-pending",
    )
    @Transactional
    fun handle(
        payload: String,
        ack: Acknowledgment,
    ) {
        val event = objectMapper.readValue(payload, SurveyPaymentPendingEvent::class.java)
        val orderId =
            paymentOrderService.issueOrder(
                surveyId = UUID.fromString(event.surveyId),
                makerId = UUID.fromString(event.makerId),
                amount = event.amount,
            )
        log.info("단독 주문 발급: surveyId={}, orderId={}", event.surveyId, orderId)
        ack.acknowledge()
    }
}
