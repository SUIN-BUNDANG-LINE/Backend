package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.OutOfTicketException
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory.REWARD_NAME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.absoluteValue
import kotlin.test.assertFalse

class DrawingTest {
    @Test
    fun `드로잉 보드의 티켓이 전부 뽑혔을 때 insertQuarter 를 실행하면 오류가 발생한다`() {
        // given
        val allSelectedDrawingBoard = DrawingBoardFixtureFactory.createAllSelectedRewardDrawingBoard()
        val drawingMachine = DrawingMachine(allSelectedDrawingBoard, 3)

        // when, then
        assertThrows<OutOfTicketException> { drawingMachine.insertQuarter() }
    }

    @Test
    fun `뽑기를 하면 DrawingBoard 에 그 결과가 반영된다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        val drawingBoardForDrawing = DrawingBoardFixtureFactory.createDrawingBoard()

        // when
        val drawingMachine = DrawingMachine(drawingBoardForDrawing, 3)
        drawingMachine.insertQuarter()
        drawingMachine.selectTicket()
        if (drawingMachine.openTicketAndCheckIsWon()) {
            drawingMachine.getRewardName()
        }

        // then
        assertEquals(drawingBoard.selectedTicketCount + 1, drawingBoardForDrawing.selectedTicketCount)
        assertFalse { drawingBoard.tickets[3].isSelected }
        assertTrue { drawingBoardForDrawing.tickets[3].isSelected }
    }

    @Test
    fun `당첨 종이를 뽑고 getRewardName 을 호출하면 리워드 이름이 출력된다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createRewardAtIndex3DrawingBoard()

        // when
        val drawingMachine = DrawingMachine(drawingBoard, 3)
        drawingMachine.insertQuarter()
        drawingMachine.selectTicket()
        drawingMachine.openTicketAndCheckIsWon()

        // then
        assertEquals(REWARD_NAME, drawingMachine.getRewardName())
    }

    @Test
    fun `이미 뽑힌 곳을 뽑으면 오류가 발생한다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createSelectedIndex3DrawingBoard()

        // when, then
        assertThrows<AlreadySelectedTicketException> {
            val drawingMachine = DrawingMachine(drawingBoard, 3)
            drawingMachine.insertQuarter()
            drawingMachine.selectTicket()
            drawingMachine.openTicketAndCheckIsWon()
        }
    }

    @Test
    fun `추첨을 loopCount 번 했을 때 기대 확률과 실제 확률 차가 0점1 미만이다`() {
        // given
        val selectedNumber = 3
        val loopCount = 500000

        // when
        val winCount =
            (1..loopCount)
                .map {
                    val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()
                    val drawingMachine = DrawingMachine(drawingBoard, selectedNumber)

                    drawingMachine.insertQuarter()
                    drawingMachine.selectTicket()
                    if (drawingMachine.openTicketAndCheckIsWon()) 1 else 0
                }.sum()

        val expectedProbability =
            DrawingBoardFixtureFactory.totalRewardCount.toDouble() / DrawingBoardFixtureFactory.SURVEY_PARTICIPANT_COUNT * 100
        val realProbability = winCount.toDouble() / loopCount * 100

        // then
        println("기대 확률 : $expectedProbability%")
        println("실제 확률 : $realProbability%")
        assertTrue(
            (expectedProbability - realProbability).absoluteValue <= 0.1,
        )
    }
}
