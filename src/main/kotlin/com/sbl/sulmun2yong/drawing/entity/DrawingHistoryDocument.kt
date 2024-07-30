package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.global.data.PhoneNumber
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "drawingHistories")
data class DrawingHistoryDocument(
    @Id
    val id: UUID,
    val participantId: UUID,
    val phoneNumber: String,
    val drawingBoardId: UUID,
    val selectedTicketIndex: Int,
    val ticket: Ticket,
) {
    companion object {
        fun of(drawingHistory: DrawingHistory) =
            DrawingHistoryDocument(
                id = drawingHistory.id,
                participantId = drawingHistory.participantId,
                phoneNumber = drawingHistory.phoneNumber.toString(),
                drawingBoardId = drawingHistory.drawingBoardId,
                selectedTicketIndex = drawingHistory.selectedTicketIndex,
                ticket = drawingHistory.ticket,
            )
    }

    fun toDomain() =
        DrawingHistory(
            id = id,
            participantId = participantId,
            phoneNumber = PhoneNumber.create(phoneNumber),
            drawingBoardId = drawingBoardId,
            selectedTicketIndex = selectedTicketIndex,
            ticket = ticket,
        )
}
