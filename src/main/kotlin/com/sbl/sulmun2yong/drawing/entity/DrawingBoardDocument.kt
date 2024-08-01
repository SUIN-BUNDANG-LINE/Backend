package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.global.entity.BaseTimeDocument
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawingBoards")
data class DrawingBoardDocument(
    @Id
    val id: UUID,
    val surveyId: UUID,
    val tickets: List<Ticket>,
) : BaseTimeDocument() {
    fun toDomain() =
        DrawingBoard(
            id = this.id,
            surveyId = this.surveyId,
            tickets = this.tickets,
        )

    companion object {
        fun of(drawingBoard: DrawingBoard): DrawingBoardDocument {
            val tickets = drawingBoard.tickets

            return DrawingBoardDocument(
                id = drawingBoard.id,
                surveyId = drawingBoard.surveyId,
                tickets = tickets,
            )
        }
    }
}
