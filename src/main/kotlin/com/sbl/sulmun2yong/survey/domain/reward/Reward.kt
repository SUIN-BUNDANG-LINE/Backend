package com.sbl.sulmun2yong.survey.domain.reward

import com.sbl.sulmun2yong.survey.exception.InvalidRewardException

data class Reward(
    val name: String,
    val category: String,
    val count: Int,
) {
    init {
        require(count > 0) { throw InvalidRewardException() }
    }

    companion object {
        const val DEFAULT_REWARD_NAME = "리워드 명"
        const val DEFAULT_REWARD_CATEGORY = "리워드 카테고리"
        const val DEFAULT_REWARD_COUNT = 1

        fun create() =
            Reward(
                name = DEFAULT_REWARD_NAME,
                category = DEFAULT_REWARD_CATEGORY,
                count = DEFAULT_REWARD_COUNT,
            )
    }
}
