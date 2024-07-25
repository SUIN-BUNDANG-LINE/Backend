package com.sbl.sulmun2yong.drawing.domain.ticket

class WinningTicket(
    private val rewardName: String,
) : Ticket {
    override val isWinningPosition = true
    override var isSelected: Boolean = false
}
