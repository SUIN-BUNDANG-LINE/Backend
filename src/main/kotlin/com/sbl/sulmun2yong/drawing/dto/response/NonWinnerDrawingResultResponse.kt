package com.sbl.sulmun2yong.drawing.dto.response

class NonWinnerDrawingResultResponse : DrawingResultResponse {
    override val isWon: Boolean = false

    companion object {
        fun create() = NonWinnerDrawingResultResponse()
    }
}
