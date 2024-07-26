package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.state.HasQuarterState
import com.sbl.sulmun2yong.drawing.domain.state.LooserState
import com.sbl.sulmun2yong.drawing.domain.state.NoQuarterState
import com.sbl.sulmun2yong.drawing.domain.state.OpenPaperState
import com.sbl.sulmun2yong.drawing.domain.state.OutOfPaperState
import com.sbl.sulmun2yong.drawing.domain.state.State
import com.sbl.sulmun2yong.drawing.domain.state.WinnerState

class DrawingMachine(
    private val drawingBoard: DrawingBoard,
    private val selectedNumber: Int,
) {
    fun getSelectedTicket() = drawingBoard.tickets[selectedNumber]

    fun getRemainingTicketCount() = drawingBoard.tickets.size - drawingBoard.selectedTicketCount

    fun setIsSelectedTrue() {
        drawingBoard.tickets[selectedNumber].isSelected = true
    }

    val noQuarterState: State = NoQuarterState(this)
    val hasQuarterState: State = HasQuarterState(this)
    val openPaperState: State = OpenPaperState(this)
    val looserState: State = LooserState(this)
    val winnerState: State = WinnerState(this)
    val outOfPaperState: State = OutOfPaperState()

    var state = noQuarterState

    fun insertQuarter() {
        state.insertQuarter()
    }

    fun selectPaper() {
        state.selectPaper()
    }

    fun openPaperAndCheckIsWon(): Boolean {
        state.openPaper()
        return state.getResult()
    }

    fun getRewardName(): String = state.getRewardName()
}
