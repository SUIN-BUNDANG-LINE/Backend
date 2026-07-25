package com.sbl.sulmun2yong.payment.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.sbl.sulmun2yong.payment.dto.TossWebhookRequest
import com.sbl.sulmun2yong.payment.service.PaymentWebhookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// 토스 웹훅 착지점 - 성공 & 중복이면 200(재발사 중지), 처리 실패면 5xx(재발사 유도)
// 멱등키는 토스가 헤더로 주는 전송 고유 ID(재전송에도 동일 유지). 원문 보존을 위해 body는 String으로 받는다.
@RestController
@RequestMapping("/api/v1/payments")
class PaymentWebhookController(
    private val paymentWebhookService: PaymentWebhookService,
    private val objectMapper: ObjectMapper,
) {
    @PostMapping("/webhook")
    fun webhook(
        @RequestHeader("tosspayments-webhook-transmission-id") transmissionId: String,
        @RequestBody body: String,
    ): ResponseEntity<Void> {
        val request = objectMapper.readValue(body, TossWebhookRequest::class.java)
        paymentWebhookService.handle(
            webhookId = transmissionId,
            eventType = request.eventType,
            orderId = request.data.orderId,
            paymentKey = request.data.paymentKey,
            status = request.data.status,
            rawPayload = body,
        )
        return ResponseEntity.ok().build()
    }
}
