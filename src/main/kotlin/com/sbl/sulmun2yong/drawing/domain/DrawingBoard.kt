package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import java.util.UUID

class DrawingBoard(
    val id: UUID,
    val surveyId: UUID,
    var selectedTicketCount: Int,
    val tickets: Array<Ticket>,
) {
    companion object {
        fun create(
            surveyId: UUID,
            boardSize: Int,
            rewards: Array<Reward>,
        ): DrawingBoard {
            val tickets =
                TicketFactory.createTickets(
                    rewards = rewards,
                    maxTicketCount = boardSize,
                )
            return DrawingBoard(
                id = UUID.randomUUID(),
                surveyId = surveyId,
                selectedTicketCount = 0,
                tickets = tickets,
            )
        }
    }
}
