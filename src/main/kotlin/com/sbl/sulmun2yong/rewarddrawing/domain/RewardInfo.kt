package com.sbl.sulmun2yong.rewarddrawing.domain

import java.util.UUID

class RewardInfo(
    val id: UUID,
    val name: String,
    val count: Int,
) {
    companion object {
        fun of(reward: Reward) =
            RewardInfo(
                id = reward.id,
                name = reward.name,
                count = reward.count,
            )
    }
}
