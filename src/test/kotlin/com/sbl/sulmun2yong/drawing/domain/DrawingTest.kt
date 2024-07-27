package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.TicketFactory
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingException
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.absoluteValue

class DrawingTest {
    @Test
    fun `비어 있는 티켓 팩토리를 만든다`() {
        val ticketFactory = TicketFactory()
    }

    @Test
    fun `뽑기를 하면 DrawingBoard 에 그 결과가 반영된다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        val testRewardName = "테스트 아이스 아메리카노"
        drawingBoard.tickets[3] = WinningTicket(testRewardName)

        // when
        val drawingMachine = DrawingMachine(drawingBoard, 3)
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.openPaperAndCheckIsWon()

        // then
        assertEquals("테스트 아이스 아메리카노", drawingMachine.getRewardName())
    }

    @Test
    fun `당첨 종이를 뽑이면 리워드 이름이 출력된다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        val testRewardName = "테스트 아이스 아메리카노"
        drawingBoard.tickets[3] = WinningTicket(testRewardName)

        // when
        val drawingMachine = DrawingMachine(drawingBoard, 3)
        drawingMachine.insertQuarter()
        drawingMachine.selectPaper()
        drawingMachine.openPaperAndCheckIsWon()

        // then
        assertEquals("테스트 아이스 아메리카노", drawingMachine.getRewardName())
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
                    drawingMachine.selectPaper()
                    if (drawingMachine.openPaperAndCheckIsWon()) 1 else 0
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

    @Test
    fun `이미 뽑힌 곳을 뽑으면 오류가 발생한다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        drawingBoard.tickets[3].isSelected = true

        // when, then
        assertThrows<InvalidDrawingException> {
            val drawingMachine = DrawingMachine(drawingBoard, 3)
            drawingMachine.insertQuarter()
            drawingMachine.selectPaper()
            drawingMachine.openPaperAndCheckIsWon()
        }
    }
}
