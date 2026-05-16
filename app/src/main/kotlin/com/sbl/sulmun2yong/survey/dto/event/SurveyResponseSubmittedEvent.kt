package com.sbl.sulmun2yong.survey.dto.event

import java.time.Instant

data class SurveyResponseSubmittedEvent(
    val eventId: String,
    val surveyId: String,
    // 응답 식별자 역할. 한 응답이 여러 ResponseEntity로 분해되므로 participantId가 응답을 대표
    val participantId: String,
    val currentParticipantCount: Int,
    val targetParticipantCount: Int?,
    val timestamp: Instant = Instant.now(),
)
