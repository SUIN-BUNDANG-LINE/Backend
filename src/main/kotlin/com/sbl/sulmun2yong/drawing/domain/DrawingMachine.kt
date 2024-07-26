package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.state.HasQuarterState
import com.sbl.sulmun2yong.drawing.domain.state.LooserState
import com.sbl.sulmun2yong.drawing.domain.state.NoQuarterState
import com.sbl.sulmun2yong.drawing.domain.state.OutOfPaperState
import com.sbl.sulmun2yong.drawing.domain.state.State
import com.sbl.sulmun2yong.drawing.domain.state.WinnerState

class DrawingMachine(
    private val drawingBoard: DrawingBoard,
    private val selectedNumber: Int,
) {
    fun getSelectedPaper() = drawingBoard.tickets[selectedNumber]

    fun getSelectedTicketCount() = drawingBoard.selectedTicketCount

    val noQuarterState: State = NoQuarterState(this)
    val hasQuarterState: State = HasQuarterState(this)
    val looserState: State = LooserState(this)
    val winnerState: State = WinnerState(this)
    val outOfPaperState: State = OutOfPaperState()

    var state = noQuarterState

    fun insertQuarter() {
        state.insertQuarter()
    }

    fun getResult(): Boolean = state.getResult()

    fun getRewardName(): String = state.getRewardName()
}
