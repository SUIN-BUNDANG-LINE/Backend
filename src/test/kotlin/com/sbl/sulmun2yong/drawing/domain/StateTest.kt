package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class StateTest {
    @Test
    fun `드로잉 보드의 티켓이 전부 뽑혔을 때 insertQuarter 를 실행하면 오류가 발생한다`() {
        // given
        val allSelectedDrawingBoard = DrawingBoardFixtureFactory.createAllSelectedRewardDrawingBoard()
        val drawingMachine = DrawingMachine(allSelectedDrawingBoard, 3)

        // when
        assertThrows<InvalidDrawingException> { drawingMachine.insertQuarter() }
    }

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
}
