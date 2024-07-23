package com.sbl.sulmun2yong.rewarddrawing.domain

class Ticket(
    val isWinningPosition: Boolean,
    var isSelected: Boolean,
    val rewardInfo: RewardInfo?,
) {
    companion object {
        fun createWiningTicket(reward: Reward) =
            Ticket(
                isWinningPosition = true,
                isSelected = false,
                rewardInfo = RewardInfo.of(reward),
            )

        fun createLosingTicket() =
            Ticket(
                isWinningPosition = false,
                isSelected = false,
                rewardInfo = null,
            )
    }
}
