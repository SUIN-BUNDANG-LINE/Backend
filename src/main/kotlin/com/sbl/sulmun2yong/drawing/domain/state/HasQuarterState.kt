package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class HasQuarterState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun selectPaper() {
        val selectedPaper = drawingMachine.getSelectedPaper()

        if (selectedPaper.isSelected) {
            drawingMachine.state = drawingMachine.noQuarterState
            throw InvalidDrawingException()
        }

        drawingMachine.setIsSelectedTrue()

        drawingMachine.state =
            if (selectedPaper is WinningTicket) drawingMachine.winnerState else drawingMachine.looserState
    }

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName() = throw InvalidDrawingException()
}
