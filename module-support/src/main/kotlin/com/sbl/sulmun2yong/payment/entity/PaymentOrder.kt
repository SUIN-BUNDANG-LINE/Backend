package com.sbl.sulmun2yong.payment.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.*
import java.util.*

// 카드 주문·결제 원장 — 설문당 1건. 토스 orderId로 결제를 식별한다.
// PENDING → confirm 성공 DONE(payment_key 기록) / 실패 FAILED / 취소 웹훅 CANCELED.
@Entity
@Table(name = "payment_orders")
class PaymentOrder(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val makerId: UUID,
    // 토스 주문 고유값(6~64자) - 재 결제 시 새 값으로 교체되므로 var
    @Column(name = "order_id", nullable = false, length = 64)
    var tossOrderId: String,
    // successUrl amount 위변조 대조 기준 - 재결제 시 경품 수 변동 반영 위해 var
    @Column(nullable = false)
    var amount: Int,
    // confirm 성공 후 채워지는 토스 결제 키
    @Column(length = 255)
    var paymentKey: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentOrderStatus = PaymentOrderStatus.PENDING,
    // 발급 출처(단독/모금) - 발급 시점 확정 불변, settled·failed 이벤트에 실려 나간다
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val origin: PaymentOrderOrigin = PaymentOrderOrigin.SOLO,
) : BaseTimeEntity() {
    fun markDone(paymentKey: String) {
        this.paymentKey = paymentKey
        this.status = PaymentOrderStatus.DONE
    }

    fun markFailed() {
        this.status = PaymentOrderStatus.FAILED
    }

    fun markCanceled() {
        this.status = PaymentOrderStatus.CANCELED
    }

    // 실패·취소된 주문을 재결제 가능 상태로 되살린다 - 토스 권장에 따라 새 orderId 부여
    fun renew(
        newOrderId: String,
        newAmount: Int,
    ) {
        this.tossOrderId = newOrderId
        this.amount = newAmount
        this.paymentKey = null
        this.status = PaymentOrderStatus.PENDING
    }

    companion object {
        fun create(
            surveyId: UUID,
            makerId: UUID,
            orderId: String,
            amount: Int,
            origin: PaymentOrderOrigin,
        ) = PaymentOrder(
            id = UUID.randomUUID(),
            surveyId = surveyId,
            makerId = makerId,
            tossOrderId = orderId,
            amount = amount,
            origin = origin,
        )
    }
}
