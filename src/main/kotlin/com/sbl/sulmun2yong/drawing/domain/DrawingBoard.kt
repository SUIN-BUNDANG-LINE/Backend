package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.domain.drawingResult.NonWinnerDrawingResult
import com.sbl.sulmun2yong.drawing.domain.drawingResult.WinnerDrawingResult
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.drawing.exception.OutOfTicketException
import java.util.UUID

class DrawingBoard(
    val id: UUID,
    val surveyId: UUID,
    val selectedTicketCount: Int,
    val tickets: List<Ticket>,
) {
    fun getDrawingResult(selectedIndex: Int): DrawingResult {
        validateTicketIsRemaining()

        val selectedTicket = this.tickets[selectedIndex]
        validateTicketIsSelected(selectedTicket)

        val changedDrawingBoard = getChangedDrawingBoard(selectedIndex)
        return when (selectedTicket) {
            is Ticket.WinningTicket ->
                WinnerDrawingResult(
                    changedDrawingBoard = changedDrawingBoard,
                    isWinner = true,
                    rewardName = selectedTicket.rewardName,
                )

            is Ticket.NonWinningTicket ->
                NonWinnerDrawingResult(
                    changedDrawingBoard = changedDrawingBoard,
                    isWinner = false,
                )
        }
    }

    private fun validateTicketIsRemaining() {
        val remainingTicketCount = this.tickets.size - this.selectedTicketCount
        if (remainingTicketCount == 0) {
            throw OutOfTicketException()
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
            selectedTicketCount = this.selectedTicketCount + 1,
            tickets = deeCopyTicketsWithChangeSelectedTrue(selectedIndex),
        )

    private fun deeCopyTicketsWithChangeSelectedTrue(selectedIndex: Int): List<Ticket> {
        val copiedTickets = mutableListOf<Ticket>()
        this.tickets.forEachIndexed { index, ticket ->
            copiedTickets.add(
                if (index == selectedIndex) {
                    when (ticket) {
                        is Ticket.WinningTicket -> ticket.copy(isSelected = true)
                        is Ticket.NonWinningTicket,
                        -> ticket.copy(isSelected = true)
                    }
                } else {
                    ticket
                },
            )
        }

        return copiedTickets.toList()
    }

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
                selectedTicketCount = 0,
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
                        Ticket.WinningTicket.create(
                            rewardName = reward.name,
                            rewardCategory = reward.category,
                        ),
                    )
                    require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
                }
            }

            repeat(maxTicketCount - tickets.size) {
                tickets.add(Ticket.NonWinningTicket.create())
            }
            tickets.shuffle()

            return tickets.toList()
        }
    }
}
