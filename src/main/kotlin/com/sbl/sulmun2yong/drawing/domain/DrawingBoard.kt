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

    override fun toString(): String {
        val maxLength = tickets.maxOfOrNull { it.toString().length } ?: 0

        val builder = StringBuilder()

        tickets.forEachIndexed { index, ticket ->
            val paddedTicket = ticket.toString().padEnd(maxLength + 1, '\u3000')
            builder.append(paddedTicket)
            if ((index + 1) % 10 == 0) {
                builder.append("\n")
            }
        }

        return builder.toString()
    }
}
