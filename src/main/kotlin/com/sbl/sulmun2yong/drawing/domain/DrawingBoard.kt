package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.domain.ticket.TicketFactory
import java.util.UUID

class DrawingBoard(
    val id: UUID,
    val selectedTicketCount: Int,
    val tickets: Array<Ticket>,
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
            return DrawingBoard(id, 0, tickets)
        }
    }

    override fun toString(): String = "DrawingBoard(id=$id, selectedTicketCount=$selectedTicketCount, tickets=${tickets.contentToString()})"
}
