package com.sbl.sulmun2yong.payment.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment_webhook_inbox")
class PaymentWebhookInbox(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,

    // 웹훅 고유 ID - 인바운드 멱등키
    @Column(nullable = false, length = 64)
    val webhookId: String,
    // 대응 주문 (토스 웹훅 payload의 orderId)
    @Column(nullable = false, length = 64)
    val orderId: String,
    // 웹훅 이벤트 종류 (PAYMENET_STATUS_CHANGED 등)
    @Column(nullable = false, length = 40)
    val eventType: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val payload: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: WebhookInboxStatus = WebhookInboxStatus.RECEIVED,
    @Column(nullable = false)
    val receivedAt: Instant,
    var processedAt: Instant? = null,
) {
    fun markProcessed() {
        this.status = WebhookInboxStatus.PROCESSED
        this.processedAt = Instant.now()
    }

    companion object {
        fun create(
            webhookId: String,
            orderId: String,
            eventType: String,
            payload: String,
        ) = PaymentWebhookInbox(
            id = UUID.randomUUID(),
            webhookId = webhookId,
            orderId = orderId,
            eventType = eventType,
            payload = payload,
            receivedAt = Instant.now(),
        )
    }
}
