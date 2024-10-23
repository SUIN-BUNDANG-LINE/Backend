package com.sbl.sulmun2yong.user.util.dummy

import kotlin.math.abs

data class ProbabilityPicker<T>(
    private val probabilityMap: Map<T, Double>,
) {
    init {
        val totalProbability = probabilityMap.values.sum()
        // 부동소수점 오차를 고려하여 1에 가까운지 확인
        require(abs(totalProbability - 1.0) < 1e-9) { "확률의 합은 1이어야 합니다." }
    }

    fun pick(): T {
        val randomValue = Math.random()
        var totalProbability = 0.0

        for ((item, probability) in probabilityMap) {
            totalProbability += probability
            if (randomValue <= totalProbability) return item
        }

        return probabilityMap.keys.last()
    }
}
