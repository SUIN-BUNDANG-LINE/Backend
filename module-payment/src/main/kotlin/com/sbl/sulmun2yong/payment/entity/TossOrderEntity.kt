package com.sbl.sulmun2yong.payment.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.payment.dto.event.ProductType
import jakarta.persistence.*
import java.util.*

// 토스 결제 시도 장부 - 시도 1회 = 행 1개(불변). 재결제는 행 갱신이 아니라 새 행 발급이라 이력이 남는다.
// PK = 토스 orderId - 시도마다 새로 뽑는 불변 값이라 자연 키를 그대로 행의 정체성으로 쓴다.
// PENDING → confirm 성공 DONE(payment_key 기록) / 실패 FAILED / 취소 웹훅 CANCELED.
@Entity
@Table(name = "toss_orders")
class TossOrderEntity(
    // 토스 주문 고유값(6~64자) - 발급 시 "ord-"+UUID 구조
    @Id
    @Column(length = 64)
    val id: String,

    // 산 물건의 좌표 - 타입이 "어느 도메인의 어느 테이블에서 해석할지"를, id 가 그 행을 가리킨다.
    // succeeded·failed 이벤트에 반사되어 상품의 주인 도메인이 자기 타입만 걸러 반응한다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val productType: ProductType,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val productId: UUID,

    // 결제한 사용자의 ID
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val payerId: UUID,

    // successUrl amount 위변조 대조 기준 - 시도마다 발급 시점에 확정되는 불변 금액
    @Column(nullable = false)
    val amount: Int,

    @Column(length = 255)
    var paymentKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TossOrderStatus = TossOrderStatus.PENDING,

) : BaseTimeEntity() {
    fun markSucceeded(paymentKey: String) {
        this.paymentKey = paymentKey
        this.status = TossOrderStatus.SUCCEEDED
    }

    fun markFailed() {
        this.status = TossOrderStatus.FAILED
    }

    fun markCanceled() {
        this.status = TossOrderStatus.CANCELED
    }

    companion object {
        fun create(
            productType: ProductType,
            productId: UUID,
            payerId: UUID,
            id: String,
            amount: Int,
        ) = TossOrderEntity(
            id = id,
            productType = productType,
            productId = productId,
            payerId = payerId,
            amount = amount,
        )
    }
}
