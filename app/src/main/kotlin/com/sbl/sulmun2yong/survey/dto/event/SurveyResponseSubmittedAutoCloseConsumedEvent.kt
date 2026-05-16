package com.sbl.sulmun2yong.survey.dto.event

// survey-response-submitted Kafka 메시지 → survey 도메인 입력 이벤트 (groupId: auto-close)
data class SurveyResponseSubmittedAutoCloseConsumedEvent(
    val surveyId: String,
    val currentParticipantCount: Int,
    val targetParticipantCount: Int?,
)
