package com.sbl.sulmun2yong.drawing.domain.ticket

import com.sbl.sulmun2yong.drawing.domain.Reward
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException

class TicketFactory {
    companion object {
        fun createTickets(
            rewards: Array<Reward>,
            maxTicketCount: Int,
        ): Array<Ticket> {
            val tickets = mutableListOf<Ticket>()
            rewards.forEach { reward ->
                repeat(reward.count) {
                    tickets.add(WinningTicket(rewardName = reward.name))
                    require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
                }
            }

            repeat(200 - tickets.size) {
                tickets.add(LosingTicket())
            }

            tickets.shuffle()
            return tickets.toTypedArray()
        }
    }
}
