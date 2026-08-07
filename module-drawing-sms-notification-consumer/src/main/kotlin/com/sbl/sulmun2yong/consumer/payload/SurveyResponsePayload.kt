package com.sbl.sulmun2yong.consumer.payload

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SurveyResponsePayload(
    val surveyId: String,
    val currentParticipantCount: Int,
    val targetParticipantCount: Int?,
)
