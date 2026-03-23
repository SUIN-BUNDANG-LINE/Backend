package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "drawing_boards")
class DrawingBoardEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @OneToMany(mappedBy = "drawingBoard", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("ticketIndex ASC")
    val tickets: MutableList<TicketEntity> = mutableListOf(),
) : BaseTimeEntity() {
    fun toDomain() =
        DrawingBoard(
            id = id,
            surveyId = surveyId,
            tickets = tickets.map { it.toDomain() },
        )

    companion object {
        fun of(drawingBoard: DrawingBoard): DrawingBoardEntity {
            val entity =
                DrawingBoardEntity(
                    id = drawingBoard.id,
                    surveyId = drawingBoard.surveyId,
                )
            drawingBoard.tickets.forEachIndexed { index, ticket ->
                entity.tickets.add(TicketEntity.from(ticket, index, entity))
            }
            return entity
        }
    }
}
