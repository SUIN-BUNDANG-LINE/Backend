package com.sbl.sulmun2yong.global.kafka.config

// 도메인 이벤트가 흘러가는 모든 Kafka 토픽의 단일 진실 원천.
// 발행 주체(`*EventPublisher`) → 이 상수를 참조 → grep 한 번으로 발행 지점 추적.
object KafkaTopics {
    const val SURVEY_RESPONSE_SUBMITTED = "survey-response-submitted"
    const val DRAWING_COMPLETED = "drawing-completed"
    const val DRAWING_NOTIFICATION_DLT = "drawing-notification.DLT"
}
