package com.sbl.sulmun2yong.drawing.domain.state

import com.sbl.sulmun2yong.drawing.domain.DrawingMachine
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException

class WinnerState(
    private val drawingMachine: DrawingMachine,
) : State {
    override fun insertQuarter() = throw InvalidDrawingException()

    override fun getResult() = throw InvalidDrawingException()

    override fun getRewardName(): String {
        val selectedTicket = drawingMachine.getSelectedPaper()
        if (selectedTicket is WinningTicket) {
            return selectedTicket.rewardName
        } else {
            throw InvalidDrawingException()
        }
    }
}
