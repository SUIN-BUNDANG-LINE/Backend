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
    val surveyId: UUID,
    val userId: UUID?,
) : BaseTimeDocument() {
    companion object {
        fun of(participant: Participant) =
            ParticipantDocument(
                id = participant.id,
                surveyId = participant.surveyId,
                userId = participant.userId,
            )
    }
}