package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.NonWinningTicket
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
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
            rewards: Array<Reward>,
            maxTicketCount: Int,
        ): Array<Ticket> {
            val tickets = mutableListOf<Ticket>()
            rewards.map { reward ->
                repeat(reward.count) {
                    tickets.add(
                        WinningTicket.create(
                            rewardName = reward.name,
                            rewardCategory = reward.category,
                        ),
                    )
                    require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
                }
            }

            repeat(maxTicketCount - tickets.size) {
                tickets.add(NonWinningTicket.create())
            }

            tickets.shuffle()
            return tickets.toTypedArray()
        }
    }
}
