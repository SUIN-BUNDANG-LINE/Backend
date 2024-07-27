package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class OpenPaperState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun selectPaper() = throw InvalidDrawingException()

    override fun openPaper() {
        val selectedTicket = drawingMachine.getSelectedTicket()
        if (selectedTicket.isWinning) {
            drawingMachine.state = drawingMachine.winnerState
        } else {
            drawingMachine.state = drawingMachine.nonWinnerState
        }
    }

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName() = throw InvalidDrawingException()
}
