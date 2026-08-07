package com.sbl.sulmun2yong.payment.controller

import com.sbl.sulmun2yong.payment.dto.response.CheckoutInfoResponse
import com.sbl.sulmun2yong.payment.repository.PaymentOrderRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 결제 페이지(checkout.html)가 로드 직후 호출하는 주문 정보 API
// clientKey 는 브라우저 노출용 공개 키. secretKey 는 절대 응답에 싣지 않는다.
@RestController
@RequestMapping("/api/v1/payments")
class PaymentCheckoutInfoController(
    private val paymentOrderRepository: PaymentOrderRepository,
    @Value("\${toss.client-key}")
    private val tossClientKey: String,
) {
    // orderId(모금 - 참여자별 주문) 또는 surveyId(단독 - 설문당 1주문) 로 주문을 찾는다.
    // 단독 개시는 이벤트로 발급되므로 수렴 전이면 404 - 프론트(checkout.html)가 재시도한다.
    @GetMapping("/checkout-info")
    fun checkoutInfo(
        @RequestParam(required = false) orderId: String?,
        @RequestParam(required = false) surveyId: String?,
    ): ResponseEntity<CheckoutInfoResponse> {
        val order =
            when {
                orderId != null -> paymentOrderRepository.findByTossOrderId(orderId).orElse(null)
                surveyId != null -> paymentOrderRepository.findBySurveyId(java.util.UUID.fromString(surveyId)).orElse(null)
                else -> null
            } ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            CheckoutInfoResponse(
                orderId = order.tossOrderId,
                amount = order.amount,
                status = order.status.name,
                clientKey = tossClientKey,
            ),
        )
    }
}
