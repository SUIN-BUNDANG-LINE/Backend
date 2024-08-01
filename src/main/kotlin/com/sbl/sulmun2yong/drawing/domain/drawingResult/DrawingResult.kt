package com.sbl.sulmun2yong.drawing.domain.drawingResult

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard

interface DrawingResult {
    val changedDrawingBoard: DrawingBoard
    val isWinner: Boolean
}
