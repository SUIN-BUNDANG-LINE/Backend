package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class HasQuarterState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun getResult(): Boolean {
        val selectedPaper = drawingMachine.getSelectedPaper()

        if (selectedPaper is WinningTicket) {
            drawingMachine.state = drawingMachine.winnerState
            return true
        } else {
            drawingMachine.looserState
            return false
        }
    }

    override fun getRewardName() = throw InvalidDrawingException()
}
