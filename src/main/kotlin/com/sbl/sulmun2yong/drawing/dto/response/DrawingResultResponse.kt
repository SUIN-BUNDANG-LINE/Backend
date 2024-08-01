package com.sbl.sulmun2yong.drawing.dto.response

sealed class DrawingResultResponse(
    val isWon: Boolean,
) {
    class Winner(
        val rewardName: String,
    ) : DrawingResultResponse(isWon = true)

    class NonWinner : DrawingResultResponse(isWon = false)
}
