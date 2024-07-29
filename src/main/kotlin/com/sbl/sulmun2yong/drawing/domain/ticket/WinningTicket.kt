package com.sbl.sulmun2yong.drawing.domain.ticket

data class WinningTicket(
    val rewardName: String,
    val rewardCategory: String,
    override var isSelected: Boolean,
) : Ticket {
    companion object {
        fun create(
            rewardName: String,
            rewardCategory: String,
        ) = WinningTicket(
            rewardName = rewardName,
            rewardCategory = rewardCategory,
            isSelected = false,
        )
    }
}
