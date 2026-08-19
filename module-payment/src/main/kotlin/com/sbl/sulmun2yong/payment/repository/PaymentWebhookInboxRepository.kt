package com.sbl.sulmun2yong.payment.repository

import com.sbl.sulmun2yong.payment.entity.PaymentWebhookInbox
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PaymentWebhookInboxRepository : JpaRepository<PaymentWebhookInbox, UUID> {
    fun existsByWebhookId(webhookId: String): Boolean
}
