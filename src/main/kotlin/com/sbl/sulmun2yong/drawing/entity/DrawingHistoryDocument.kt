package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawingHistories")
data class DrawingHistoryDocument(
    @Id
    val id: UUID,
    val participantId: UUID,
    val drawingBoardId: UUID,
    val selectedTicketIndex: Int,
    val ticket: Ticket,
) {
    companion object {
        fun of(drawingHistory: DrawingHistory) =
            DrawingHistoryDocument(
                id = drawingHistory.id,
                participantId = drawingHistory.participantId,
                drawingBoardId = drawingHistory.drawingBoardId,
                selectedTicketIndex = drawingHistory.selectedTicketIndex,
                ticket = drawingHistory.ticket,
            )
    }

    fun toDomain() =
        DrawingHistory(
            id = id,
            participantId = participantId,
            drawingBoardId = drawingBoardId,
            selectedTicketIndex = selectedTicketIndex,
            ticket = ticket,
        )
}
