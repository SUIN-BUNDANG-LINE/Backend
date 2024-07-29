package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import java.util.UUID

class DrawingHistory(
    val id: UUID,
    val participantId: UUID,
    val drawingBoardId: UUID,
    val selectedTicketIndex: Int,
    val ticket: Ticket,
) {
    companion object {
        fun create(
            participantId: UUID,
            drawingBoardId: UUID,
            selectedTicketIndex: Int,
            ticket: Ticket,
        ): DrawingHistory =
            DrawingHistory(
                UUID.randomUUID(),
                participantId,
                drawingBoardId,
                selectedTicketIndex,
                ticket,
            )
    }
}
