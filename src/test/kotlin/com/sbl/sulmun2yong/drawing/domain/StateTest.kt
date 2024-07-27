package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.NonWinningTicket
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class StateTest {
    @Test
    fun `noQuarterState 에서 insertQuarter() 제외 메소드를 호출 하면 오류가 발생한다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val noQuarterState = drawingMachine.noQuarterState

        // when

        // then
        assertDoesNotThrow { noQuarterState.insertQuarter() }
        assertThrows<InvalidDrawingException> { noQuarterState.getResult() }
        assertThrows<InvalidDrawingException> { noQuarterState.openPaper() }
        assertThrows<InvalidDrawingException> { noQuarterState.selectPaper() }
        assertThrows<InvalidDrawingException> { noQuarterState.getRewardName() }
    }

    @Test
    fun `HasQuarterState 에서 insertQuarter() 제외 메소드를 호출 하면 오류가 발생한다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val hasQuarterState = drawingMachine.hasQuarterState

        // when
        drawingMachine.insertQuarter()

        // then
        assertThrows<InvalidDrawingException> { hasQuarterState.insertQuarter() }
        assertDoesNotThrow { hasQuarterState.selectPaper() }
        assertThrows<InvalidDrawingException> { hasQuarterState.openPaper() }
        assertThrows<InvalidDrawingException> { hasQuarterState.getResult() }
        assertThrows<InvalidDrawingException> { hasQuarterState.getRewardName() }
    }

    @Test
    fun `HasQuarterState 에서 이미 선택한 티켓에 대해 insertQuarter()를 호출하면 오류가 발생한다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createRewardAtIndex3DrawingBoard()
        rewardBoard.tickets[3].isSelected = true
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val hasQuarterState = drawingMachine.hasQuarterState

        // when
        drawingMachine.insertQuarter()

        // then
        assertThrows<AlreadySelectedTicketException> { hasQuarterState.selectPaper() }
    }

    @Test
    fun `OpenPaperState 에서 insertQuarter() 제외 메소드를 호출 하면 오류가 발생한다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val openPaperState = drawingMachine.openPaperState

        // when
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()

        // then
        assertThrows<InvalidDrawingException> { openPaperState.insertQuarter() }
        assertThrows<InvalidDrawingException> { openPaperState.selectPaper() }
        assertDoesNotThrow { openPaperState.openPaper() }
        assertThrows<InvalidDrawingException> { openPaperState.getResult() }
        assertThrows<InvalidDrawingException> { openPaperState.getRewardName() }
    }

    @Test
    fun `WinnerState 에서 getResult(), getRewardName() 제외 메소드를 호출 하면 오류가 발생한다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createRewardAtIndex3DrawingBoard()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val winnerState = drawingMachine.winnerState

        // when
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.state.openPaper()

        // then
        assertThrows<InvalidDrawingException> { winnerState.insertQuarter() }
        assertThrows<InvalidDrawingException> { winnerState.selectPaper() }
        assertThrows<InvalidDrawingException> { winnerState.openPaper() }
        assertDoesNotThrow { winnerState.getResult() }
        assertDoesNotThrow { winnerState.getRewardName() }
    }

    @Test
    fun `WinnerState 에서 NonWinningTicket 인데 getRewardName 을 호출하면 오류가난다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createRewardAtIndex3DrawingBoard()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val winnerState = drawingMachine.winnerState

        // when
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.openPaperAndCheckIsWon()
        rewardBoard.tickets[3] = NonWinningTicket()

        // then
        assertThrows<InvalidDrawingException> { winnerState.getRewardName() }
    }

    @Test
    fun `NonWinnerState 에서 getResult() 제외 메소드를 호출 하면 오류가 발생한다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createNoRewardAtIndex3DrawingBoardRemainOne()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val nonWinnerState = drawingMachine.nonWinnerState

        // when
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.state.openPaper()

        // then
        assertThrows<InvalidDrawingException> { nonWinnerState.insertQuarter() }
        assertThrows<InvalidDrawingException> { nonWinnerState.selectPaper() }
        assertThrows<InvalidDrawingException> { nonWinnerState.openPaper() }
        assertDoesNotThrow { nonWinnerState.getResult() }
        assertThrows<InvalidDrawingException> { nonWinnerState.getRewardName() }
    }

    @Test
    fun `OutOfPaperState 에서 어떤 메소드를 호출하든 오류가 발생한다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createRewardAtIndex3DrawingBoardRemainOne()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val outOfPaperState = drawingMachine.outOfPaperState

        // when
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.openPaperAndCheckIsWon()
        drawingMachine.getRewardName()

        // then
        assertThrows<InvalidDrawingException> { outOfPaperState.insertQuarter() }
        assertThrows<InvalidDrawingException> { outOfPaperState.selectPaper() }
        assertThrows<InvalidDrawingException> { outOfPaperState.openPaper() }
        assertThrows<InvalidDrawingException> { outOfPaperState.getResult() }
        assertThrows<InvalidDrawingException> { outOfPaperState.getRewardName() }
    }

    @Test
    fun `리워드가 하나 남았을 때는 WinnerState 이후 OutOfPaperState가 된다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createRewardAtIndex3DrawingBoardRemainOne()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val winnerState = drawingMachine.winnerState

        // when
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.openPaperAndCheckIsWon()
        drawingMachine.getRewardName()

        // then
        assertEquals(drawingMachine.outOfPaperState, drawingMachine.state)
    }

    @Test
    fun `리워드가 하나 남았을 때는 nonWinnerState 이후 OutOfPaperState가 된다`() {
        // given
        val rewardBoard = DrawingBoardFixtureFactory.createNoRewardAtIndex3DrawingBoardRemainOne()
        val drawingMachine = DrawingMachine(rewardBoard, 3)
        val nonWinnerState = drawingMachine.nonWinnerState

        // when
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.openPaperAndCheckIsWon()

        // then
        assertEquals(drawingMachine.outOfPaperState, drawingMachine.state)
    }
}
