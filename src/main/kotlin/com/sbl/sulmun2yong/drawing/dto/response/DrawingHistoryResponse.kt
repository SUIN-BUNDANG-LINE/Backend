package com.sbl.sulmun2yong.drawing.dto.response

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.ticket.TicketEntity
import java.util.UUID

class DrawingHistoryResponse(
    val id: UUID,
    val participantId: UUID,
    val phoneNumber: String,
    val selectedTicketIndex: Int,
    val ticketEntity: TicketEntity,
) {
    companion object {
        fun of(drawingHistory: DrawingHistory) =
            DrawingHistoryResponse(
                id = drawingHistory.id,
                participantId = drawingHistory.participantId,
                phoneNumber = drawingHistory.phoneNumber.value,
                selectedTicketIndex = drawingHistory.selectedTicketIndex,
                ticketEntity = drawingHistory.ticketEntity,
            )
    }
}
