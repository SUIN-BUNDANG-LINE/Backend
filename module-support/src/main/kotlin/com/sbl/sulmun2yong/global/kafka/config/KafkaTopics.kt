package com.sbl.sulmun2yong.global.kafka.config

// 도메인 이벤트가 흘러가는 모든 Kafka 토픽의 단일 진실 원천.
// 발행 주체(`*EventPublisher`) → 이 상수를 참조 → grep 한 번으로 발행 지점 추적.
object KafkaTopics {
    const val DRAWING_COMPLETED = "drawing-completed"
    const val DRAWING_NOTIFICATION_DLT = "drawing-notification.DLT"
    const val CO_PAYMENT_SETTLED = "co-payment-settled"
    const val CO_FUNDING_FAILED = "co-funding-failed"
    const val CO_FUNDING_CREATED = "co-funding-created"
    const val PAYMENT_SETTLED = "payment-settled"
    const val PAYMENT_FAILED = "payment-failed"
    const val PAYMENT_REFUNDED = "payment-refunded"
    const val PAYMENT_CANCEL_REQUESTED = "payment-cancel-requested"
    const val CO_FUNDING_CONFIRMED = "co-funding-confirmed"
    const val CO_FUNDING_REQUESTED = "co-funding-requested"

    // 개설 판정 결과(승인·거절) - 한 판정의 두 결과라 토픽 하나에 verdict 로 싣는다
    const val CO_FUNDING_REVIEWED = "co-funding-reviewed"
    const val SURVEY_PAYMENT_PENDING = "survey-payment-pending"

    // 사가 리스너 실패의 통합 죽은 편지 큐 - 원본 토픽은 kafka_dlt-original-topic 헤더가 보존한다
    const val SAGA_DLT = "saga.DLT"
}
