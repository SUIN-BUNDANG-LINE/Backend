package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.domain.ticket.TicketFactory
import java.util.UUID

class DrawingBoard(
    val id: UUID,
    val size: Int,
    private val tickets: Array<Ticket>,
) {
    private val isFinished = false

    companion object {
        fun create(
            id: UUID,
            boardSize: Int,
            rewards: Array<Reward>,
        ): DrawingBoard {
            val tickets =
                TicketFactory.createTickets(
                    rewards = rewards,
                    maxTicketCount = boardSize,
                )
            return DrawingBoard(id, boardSize, tickets)
        }
    }
}
