package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.ticket.TicketEntity
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import java.util.UUID

@Entity
class DrawingBoardEntity(
    @Id
    val id: UUID,
    val surveyId: UUID,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "drawing_id")
    val ticketEntities: MutableList<TicketEntity>,
) : BaseTimeDocument() {
    fun toDomain() =
        DrawingBoard(
            id = this.id,
            surveyId = this.surveyId,
            ticketEntities = this.ticketEntities,
        )

    companion object {
        fun of(drawingBoard: DrawingBoard): DrawingBoardEntity {
            val tickets = drawingBoard.ticketEntities

            return DrawingBoardEntity(
                id = drawingBoard.id,
                surveyId = drawingBoard.surveyId,
                ticketEntities = tickets as MutableList<TicketEntity>,
            )
        }
    }
}
