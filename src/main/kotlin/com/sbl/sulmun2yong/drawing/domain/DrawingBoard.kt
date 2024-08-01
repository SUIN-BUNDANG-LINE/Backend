package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.exception.OutOfTicketException
import java.util.UUID

class DrawingBoard(
    val id: UUID,
    val surveyId: UUID,
    val tickets: List<Ticket>,
) {
    val selectedTicketCount: Int

    init {
        selectedTicketCount = calcSelectedTicketCount()
        val remainingTicketCount = this.tickets.size - selectedTicketCount

        if (remainingTicketCount <= 0) {
            throw OutOfTicketException()
        }
    }

    fun getDrawingResult(selectedIndex: Int): DrawingResult {
        val selectedTicket = this.tickets[selectedIndex]
        validateTicketIsSelected(selectedTicket)

        val changedDrawingBoard = getChangedDrawingBoard(selectedIndex)
        return when (selectedTicket) {
            is Ticket.Winning ->
                DrawingResult.Winner(
                    changedDrawingBoard = changedDrawingBoard,
                    rewardName = selectedTicket.rewardName,
                )

            is Ticket.NonWinning ->
                DrawingResult.NonWinner(
                    changedDrawingBoard = changedDrawingBoard,
                )
        }
    }

    private fun validateTicketIsSelected(selectedTicket: Ticket) {
        if (selectedTicket.isSelected) {
            throw AlreadySelectedTicketException()
        }
    }

    private fun getChangedDrawingBoard(selectedIndex: Int): DrawingBoard =
        DrawingBoard(
            id = this.id,
            surveyId = this.surveyId,
            tickets = deepCopyTicketsWithChangeSelectedTrue(selectedIndex),
        )

    private fun deepCopyTicketsWithChangeSelectedTrue(selectedIndex: Int): List<Ticket> {
        val copiedTickets = mutableListOf<Ticket>()
        this.tickets.forEachIndexed { index, ticket ->
            copiedTickets.add(
                if (index == selectedIndex) {
                    when (ticket) {
                        is Ticket.Winning -> ticket.copy(isSelected = true)
                        is Ticket.NonWinning,
                        -> ticket.copy(isSelected = true)
                    }
                } else {
                    ticket
                },
            )
        }

        return copiedTickets.toList()
    }

    private fun calcSelectedTicketCount(): Int = this.tickets.count { it.isSelected }

    companion object {
        fun create(
            surveyId: UUID,
            boardSize: Int,
            rewards: List<Reward>,
        ): DrawingBoard {
            val tickets =
                createTickets(
                    rewards = rewards,
                    maxTicketCount = boardSize,
                )
            return DrawingBoard(
                id = UUID.randomUUID(),
                surveyId = surveyId,
                tickets = tickets,
            )
        }

        private fun createTickets(
            rewards: List<Reward>,
            maxTicketCount: Int,
        ): List<Ticket> {
            val tickets = mutableListOf<Ticket>()
            rewards.map { reward ->
                repeat(reward.count) {
                    tickets.add(
                        Ticket.Winning.create(
                            rewardName = reward.name,
                            rewardCategory = reward.category,
                        ),
                    )
                    require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
                }
            }

            repeat(maxTicketCount - tickets.size) {
                tickets.add(Ticket.NonWinning.create())
            }
            tickets.shuffle()

            return tickets.toList()
        }
    }
}
