package com.sbl.sulmun2yong.payment.controller

import com.sbl.sulmun2yong.payment.service.ConfirmOutcome
import com.sbl.sulmun2yong.payment.service.PaymentConfirmService
import com.sbl.sulmun2yong.payment.service.PaymentFailService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 토스 결제창이 사용자 브라우저를 돌려보내는 착지점.
// 처리 후 결과 페이지로 302 리다이렉트 - F5 를 눌러도 confirm URL 이 아니라 결과 페이지만 재요청된다.
@RestController
@RequestMapping("/api/v1/payments")
class PaymentResultController(
    private val paymentConfirmService: PaymentConfirmService,
    private val paymentFailService: PaymentFailService,
) {
    @GetMapping("/success")
    fun success(
        @RequestParam paymentKey: String,
        @RequestParam orderId: String,
        @RequestParam amount: Int,
    ): ResponseEntity<Void> {
        val outCome = paymentConfirmService.handleSuccess(paymentKey, orderId, amount)
        val state =
            when (outCome) {
                ConfirmOutcome.DONE -> "done"
                ConfirmOutcome.PROCESSING -> "processing"
                ConfirmOutcome.FAILED -> "failed"
            }

        return redirectToResult(state, orderId)
    }

    @GetMapping("/fail")
    fun fail(
        @RequestParam orderId: String,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) message: String?,
    ): ResponseEntity<Void> {
        paymentFailService.handleFail(orderId, code)
        return redirectToResult("failed", orderId)
    }

    private fun redirectToResult(
        state: String,
        orderId: String,
    ): ResponseEntity<Void> =
        ResponseEntity
            .status(HttpStatus.FOUND)
            .header(
                HttpHeaders.LOCATION,
                "/payments/result.html?state=$state&orderId=$orderId",
            ).build()

}
