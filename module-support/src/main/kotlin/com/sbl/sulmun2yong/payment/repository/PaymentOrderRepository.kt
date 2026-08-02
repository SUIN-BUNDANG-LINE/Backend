package com.sbl.sulmun2yong.payment.repository

import com.sbl.sulmun2yong.payment.entity.PaymentOrder
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PaymentOrderRepository : JpaRepository<PaymentOrder, UUID> {
    // successUrl, 웹훅에서 orderId로 주문 조회
    fun findByTossOrderId(orderId: String): Optional<PaymentOrder>

    // startSurvey 재시작 멱등 - 기존 PENDING 주문 재사용
    fun findBySurveyId(surveyId: UUID): Optional<PaymentOrder>
}
