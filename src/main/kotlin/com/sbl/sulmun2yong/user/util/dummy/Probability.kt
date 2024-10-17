package com.sbl.sulmun2yong.user.util.dummy

data class Probability(
    private val chance: Double,
) {
    init {
        require(chance in 0.0..1.0) { "확률은 0과 1 사이의 값이어야 합니다." }
    }

    fun isWinning(): Boolean = Math.random() <= chance
}
