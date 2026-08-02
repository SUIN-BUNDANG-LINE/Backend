package com.sbl.sulmun2yong.payment.controller

import com.sbl.sulmun2yong.payment.dto.IssueOrderRequest
import com.sbl.sulmun2yong.payment.dto.IssueOrderResponse
import com.sbl.sulmun2yong.payment.service.PaymentOrderService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 서비스간 내부 API - 설문(단독 결제 개시)의 주문 발급 요청.
// /internal/** 은 게이트웨이에 라우트가 없어 외부에서 접근 불가 - 내부망 + 비밀 헤더(GatewayOnlyFilter)로 지킨다.
@RestController
@RequestMapping("/internal/payments")
class InternalPaymentOrderController(
    private val paymentOrderService: PaymentOrderService,
) {
    @PostMapping("/orders")
    fun issueOrder(
        @RequestBody request: IssueOrderRequest,
    ): ResponseEntity<IssueOrderResponse> =
        ResponseEntity.ok(
            IssueOrderResponse(
                orderId = paymentOrderService.issueOrder(request.surveyId, request.makerId, request.amount),
            ),
        )
}
