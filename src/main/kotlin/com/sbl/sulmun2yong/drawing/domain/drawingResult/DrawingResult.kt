package com.sbl.sulmun2yong.drawing.domain.drawingResult

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard

sealed class DrawingResult {
    abstract val changedDrawingBoard: DrawingBoard

    class Winner(
        val rewardName: String,
        override val changedDrawingBoard: DrawingBoard,
    ) : DrawingResult()

    class NonWinner(
        override val changedDrawingBoard: DrawingBoard,
    ) : DrawingResult()
}
