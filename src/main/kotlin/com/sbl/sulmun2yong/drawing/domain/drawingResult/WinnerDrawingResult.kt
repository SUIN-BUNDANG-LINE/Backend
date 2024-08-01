package com.sbl.sulmun2yong.drawing.domain.drawingResult

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard

class WinnerDrawingResult(
    override val changedDrawingBoard: DrawingBoard,
    val rewardName: String,
    override val isWinner: Boolean,
) : DrawingResult
