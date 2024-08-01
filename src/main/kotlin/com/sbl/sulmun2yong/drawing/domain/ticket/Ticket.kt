package com.sbl.sulmun2yong.drawing.domain.ticket

sealed class Ticket {
    abstract val isSelected: Boolean

    data class Winning(
        val rewardName: String,
        val rewardCategory: String,
        override val isSelected: Boolean,
    ) : Ticket() {
        companion object {
            fun create(
                rewardName: String,
                rewardCategory: String,
            ): Ticket =
                Winning(
                    rewardName = rewardName,
                    rewardCategory = rewardCategory,
                    isSelected = false,
                )
        }
    }

    data class NonWinning(
        override val isSelected: Boolean,
    ) : Ticket() {
        companion object {
            fun create(): Ticket =
                NonWinning(
                    isSelected = false,
                )
        }
    }
}
