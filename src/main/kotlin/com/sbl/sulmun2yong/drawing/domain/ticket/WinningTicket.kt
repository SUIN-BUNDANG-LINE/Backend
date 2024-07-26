package com.sbl.sulmun2yong.drawing.domain.ticket

class WinningTicket(
    val rewardName: String,
) : Ticket {
    override var isSelected: Boolean = false
    override val isWinning: Boolean = true

    override fun toString(): String = rewardName
}
