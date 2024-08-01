package com.sbl.sulmun2yong.drawing.domain.drawingResult

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard

class NonWinnerDrawingResult(
    override val changedDrawingBoard: DrawingBoard,
    override val isWinner: Boolean,
) : DrawingResult
