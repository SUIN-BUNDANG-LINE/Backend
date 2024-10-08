package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.global.data.PhoneNumber
import java.util.UUID

class DrawingHistory(
    val id: UUID,
    val participantId: UUID,
    val phoneNumber: PhoneNumber,
    val surveyId: UUID,
    val selectedTicketIndex: Int,
    val ticket: Ticket,
) {
    companion object {
        fun create(
            participantId: UUID,
            phoneNumber: PhoneNumber,
            surveyId: UUID,
            selectedTicketIndex: Int,
            ticket: Ticket,
        ): DrawingHistory =
            DrawingHistory(
                UUID.randomUUID(),
                participantId,
                phoneNumber,
                surveyId,
                selectedTicketIndex,
                ticket,
            )
    }
}
