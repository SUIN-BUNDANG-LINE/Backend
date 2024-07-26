package com.sbl.sulmun2yong.drawing.domain.ticket

class WinningTicket(
    val rewardName: String,
) : Ticket {
    override var isSelected: Boolean = false

    override fun toString(): String = rewardName
}
