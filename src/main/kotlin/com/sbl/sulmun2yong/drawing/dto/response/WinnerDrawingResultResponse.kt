package com.sbl.sulmun2yong.drawing.dto.response

class WinnerDrawingResultResponse(
    val rewardName: String,
) : DrawingResultResponse {
    override val isWon: Boolean = true
}
