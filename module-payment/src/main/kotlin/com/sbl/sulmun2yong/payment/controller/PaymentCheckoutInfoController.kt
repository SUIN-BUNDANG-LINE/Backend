package com.sbl.sulmun2yong.payment.controller

import com.sbl.sulmun2yong.payment.dto.response.CheckoutInfoResponse
import com.sbl.sulmun2yong.payment.entity.TossOrderEntity
import com.sbl.sulmun2yong.payment.repository.TossOrderRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 결제 페이지(checkout.html)가 로드 직후 호출하는 주문 정보 API
// clientKey 는 브라우저 노출용 공개 키. secretKey 는 절대 응답에 싣지 않는다.
// 진입은 orderId 단일 - 단독도 1인 모금 접수를 거쳐 my-order 로 자기 orderId 를 받는다.
// status 는 그대로 돌려주고 해석은 프론트 몫: PENDING·FAILED = 결제 가능(FAILED 는 같은 orderId 재시도),
// SUCCEEDED·CANCELED = 종착이라 결제창이 문을 닫는다.
@RestController
@RequestMapping("/api/v1/payments/checkout-info")
class PaymentCheckoutInfoController(
    private val tossOrderRepository: TossOrderRepository,
    @Value("\${toss.client-key}")
    private val tossClientKey: String,
) {
    // 모금 - 참여자별 주문. orderId 는 개설 시 사전 발급되어 프론트가 들고 온다.
    @GetMapping("/by-order/{orderId}")
    fun byOrder(
        @PathVariable orderId: String,
    ): ResponseEntity<CheckoutInfoResponse> = respond(tossOrderRepository.findById(orderId).orElse(null))

    private fun respond(order: TossOrderEntity?): ResponseEntity<CheckoutInfoResponse> =
        order
            ?.let {
                ResponseEntity.ok(
                    CheckoutInfoResponse(
                        orderId = it.id,
                        amount = it.amount,
                        status = it.status.name,
                        clientKey = tossClientKey,
                    ),
                )
            } ?: ResponseEntity.notFound().build()
}
