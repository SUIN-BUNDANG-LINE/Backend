package com.sbl.sulmun2yong.survey.entity

import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "participants")
class Participant(
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
        fun create(
            visitorId: String,
            surveyId: UUID,
            userId: UUID?,
        ) = Participant(UUID.randomUUID(), visitorId, surveyId, userId)
    }
}
