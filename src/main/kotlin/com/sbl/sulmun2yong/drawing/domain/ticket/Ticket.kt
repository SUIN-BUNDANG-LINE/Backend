package com.sbl.sulmun2yong.drawing.domain.ticket

sealed class Ticket {
    abstract val isSelected: Boolean

    data class WinningTicket(
        val rewardName: String,
        val rewardCategory: String,
        override val isSelected: Boolean,
    ) : Ticket() {
        companion object {
            fun create(
                rewardName: String,
                rewardCategory: String,
            ): Ticket =
                WinningTicket(
                    rewardName = rewardName,
                    rewardCategory = rewardCategory,
                    isSelected = false,
                )
        }
    }

    data class NonWinningTicket(
        override val isSelected: Boolean,
    ) : Ticket() {
        companion object {
            fun create(): Ticket =
                NonWinningTicket(
                    isSelected = false,
                )
        }
    }
}
