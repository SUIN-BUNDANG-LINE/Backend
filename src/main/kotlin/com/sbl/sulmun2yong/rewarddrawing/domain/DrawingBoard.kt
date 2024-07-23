package com.sbl.sulmun2yong.rewarddrawing.domain

import com.sbl.sulmun2yong.rewarddrawing.exception.InvalidDrawingBoardException
import java.util.UUID

class DrawingBoard(
    private val id: UUID,
    private val size: Int,
    private var selectedTicketCount: Int,
    private val tickets: Array<Ticket>,
    private val isFinished: Boolean,
) {
    fun selectTicket(selectedNumber: Int): Ticket {
        require(!isFinished) { throw InvalidDrawingBoardException() }
        require(selectedTicketCount < size) { throw InvalidDrawingBoardException() }
        tickets[selectedNumber].isSelected = true
        selectedTicketCount++

        return tickets[selectedNumber]
    }

    fun increaseSelectedTicketCount() {
        require(!isFinished) { throw InvalidDrawingBoardException() }
        require(selectedTicketCount < size) { throw InvalidDrawingBoardException() }
        selectedTicketCount++
    }

    companion object {
        fun create(
            id: UUID,
            size: Int = 200,
            rewards: List<Reward>,
        ): DrawingBoard =
            DrawingBoard(
                id = id,
                selectedTicketCount = 0,
                size = size,
                tickets = createTickets(rewards, size),
                isFinished = false,
            )

        private fun createTickets(
            rewards: List<Reward>,
            boardSize: Int,
        ): Array<Ticket> {
            val tickets = mutableListOf<Ticket>()
            for (reward in rewards) {
                for (i in 1..reward.count) {
                    tickets.add(Ticket.createWiningTicket(reward))
                    require(tickets.size <= boardSize) { throw InvalidDrawingBoardException() }
                }
            }
            for (i in 1..200 - tickets.size) {
                tickets.add(Ticket.createLosingTicket())
            }
            tickets.shuffle()

            return tickets.toTypedArray()
        }
    }
}
