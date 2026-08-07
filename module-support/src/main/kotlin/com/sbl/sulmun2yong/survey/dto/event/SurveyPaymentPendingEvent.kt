package com.sbl.sulmun2yong.survey.dto.event

import java.time.Instant

// survey-payment-pending wire 계약 - 단독(비모금) 설문이 결제 대기에 진입한 사실
// (발행: startSurvey tx 의 Outbox - PENDING_PAYMENT 전이·보드 생성과 원자).
// 구독: 결제(주문 발급, origin=SOLO - 설문당 1행 멱등). 재호출 시 재발행해도 발급이 멱등이라 안전하다.
// checkoutUrl 은 surveyId 로 조립되므로 설문은 orderId 를 알 필요가 없다(주문 식별은 결제 자기 데이터).
data class SurveyPaymentPendingEvent(
    val eventId: String,
    val surveyId: String,
    val makerId: String,
    val amount: Int,
    val occurredAt: Instant,
)
