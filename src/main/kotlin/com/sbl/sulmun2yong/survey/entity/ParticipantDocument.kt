package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import com.sbl.sulmun2yong.survey.domain.Participant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "participants")
data class ParticipantDocument(
    @Id
    val id: UUID,
    val visitorId: String,
    val surveyId: UUID,
    val userId: UUID?,
) : BaseTimeDocument() {
    companion object {
        fun of(participant: Participant) =
            ParticipantDocument(
                id = participant.id,
                visitorId = participant.visitorId,
                surveyId = participant.surveyId,
                userId = participant.userId,
            )
    }

    fun toDomain() =
        Participant(
            id = this.id,
            visitorId = this.visitorId,
            surveyId = this.surveyId,
            userId = this.userId,
            createdAt = this.createdAt,
        )
}
