package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.state.HasQuarterState
import com.sbl.sulmun2yong.drawing.domain.state.NoQuarterState
import com.sbl.sulmun2yong.drawing.domain.state.OutOfPaperState
import com.sbl.sulmun2yong.drawing.domain.state.State
import com.sbl.sulmun2yong.drawing.domain.state.WinnerState

class DrawingMachine(
    private val drawingBoard: DrawingBoard,
    val selectedNumber: Int,
) {
    private val noQuarterState: State = NoQuarterState(this)
    private val hasQuarterState: State = HasQuarterState(this)
    val looseState: State = WinnerState(this)
    val winnerState: State = WinnerState(this)
    val outOfPaperState: State = OutOfPaperState(this)

    val state = noQuarterState

    fun insertQuarter() {
        state.insertQuarter()
    }

    fun openPaper() {
        state.openPaper()
    }

    fun getResult() {
        state.getResult()
    }
}
