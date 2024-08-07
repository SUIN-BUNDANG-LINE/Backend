package com.sbl.sulmun2yong.survey.domain

import com.sbl.sulmun2yong.survey.exception.InvalidRewardException

data class Reward(
    val name: String,
    val category: String,
    val count: Int,
) {
    init {
        require(count > 0) { throw InvalidRewardException() }
    }
}
