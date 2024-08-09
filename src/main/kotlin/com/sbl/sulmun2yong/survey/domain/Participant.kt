package com.sbl.sulmun2yong.survey.domain

import java.util.UUID

data class Participant(
    val id: UUID,
    val visitorId: String,
    val surveyId: UUID,
    val userId: UUID?,
) {
    companion object {
        fun create(
            visitorId: String,
            surveyId: UUID,
            userId: UUID?,
        ) = Participant(UUID.randomUUID(), visitorId, surveyId, userId)
    }
}
