package com.sbl.sulmun2yong.drawing.domain.ticket

class WinningTicket(
    val rewardName: String,
    override var isSelected: Boolean,
) : Ticket {
    companion object {
        fun create(rewardName: String) =
            WinningTicket(
                rewardName = rewardName,
                isSelected = false,
            )
    }

    override fun toString(): String = rewardName
}
