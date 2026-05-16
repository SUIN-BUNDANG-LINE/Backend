package com.sbl.sulmun2yong.survey.dto.event

// survey-response-submitted Kafka 메시지 → survey 도메인 입력 이벤트 (groupId: response-stats)
// 동일 토픽을 다른 groupId로 fan-out 구독하여 응답 통계를 별도로 집계하기 위한 이벤트
data class SurveyResponseSubmittedStatsConsumedEvent(
    val surveyId: String,
    val currentParticipantCount: Int,
)
