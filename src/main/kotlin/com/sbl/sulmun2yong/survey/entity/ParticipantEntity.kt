package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.survey.domain.Participant
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "participants")
class ParticipantEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false)
    val visitorId: String,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @Column(columnDefinition = "BINARY(16)")
    val userId: UUID?,
) : BaseTimeEntity() {
    companion object {
        fun of(participant: Participant) =
            ParticipantEntity(
                id = participant.id,
                visitorId = participant.visitorId,
                surveyId = participant.surveyId,
                userId = participant.userId,
            )
    }

    fun toDomain() =
        Participant(
            id = id,
            visitorId = visitorId,
            surveyId = surveyId,
            userId = userId,
            createdAt = Date.from(createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant()),
        )
}
