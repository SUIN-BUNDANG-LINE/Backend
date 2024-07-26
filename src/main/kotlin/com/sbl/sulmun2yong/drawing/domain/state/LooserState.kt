package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class LooserState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun getResult(): Boolean {
        drawingMachine.state =
            if (drawingMachine.getSelectedTicketCount() > 1) {
                drawingMachine.outOfPaperState
            } else {
                drawingMachine.noQuarterState
            }
        return false
    }

    override fun getRewardName() = throw InvalidDrawingException()
}
