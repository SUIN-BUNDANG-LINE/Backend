package com.sbl.sulmun2yong.drawing.domain.drawingResult

sealed class DrawingResult {
    abstract val remainingTickets: Int

    class Winner(
        val rewardName: String,
        val rewardCategory: String,
        override val remainingTickets: Int,
    ) : DrawingResult()

    class NonWinner(
        override val remainingTickets: Int,
    ) : DrawingResult()
}
