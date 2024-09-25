package com.sbl.sulmun2yong.survey.domain

import java.util.Date
import java.util.UUID

data class Participant(
    val id: UUID,
    val surveyId: UUID,
    val userId: UUID?,
    val createdAt: Date,
) {
    companion object {
        fun create(
            surveyId: UUID,
            userId: UUID?,
        ) = Participant(UUID.randomUUID(), surveyId, userId, Date())
    }
}
