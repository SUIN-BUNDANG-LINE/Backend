package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.absoluteValue
import kotlin.test.assertFalse

class DrawingTest {
    @Test
    fun `꽝 티켓을을 뽑으면 DrawingResultNonWinner 도메인이 만들어지고  DrawingBoard 에 그 결과가 반영된다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoardRewardNotExistsIndex3()

        // when
        val drawingResult = drawingBoard.getDrawingResult(3)
        val changedDrawingBoard = drawingResult.changedDrawingBoard
        // then
        assertTrue { drawingResult is DrawingResult.NonWinner }
        assertEquals(drawingBoard.selectedTicketCount + 1, changedDrawingBoard.selectedTicketCount)
        assertFalse { drawingBoard.tickets[3].isSelected }
        assertTrue { changedDrawingBoard.tickets[3].isSelected }
    }

    @Test
    fun `당첨 티켓을 뽑으면 DrawingResultWinner 도메인이 만들어지고 rewardName 을 가져올 수 있다 그리고 DrawingBoard 에 그 결과가 반영된다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoardRewardExistsIndex3()

        // when
        val drawingResult = drawingBoard.getDrawingResult(3)
        val changedDrawingBoard = drawingResult.changedDrawingBoard
        // then
        assertTrue { drawingResult is DrawingResult.Winner }
        assertEquals(DrawingBoardFixtureFactory.REWARD_NAME, (drawingResult as DrawingResult.Winner).rewardName)
        assertEquals(drawingBoard.selectedTicketCount + 1, changedDrawingBoard.selectedTicketCount)
        assertFalse { drawingBoard.tickets[3].isSelected }
        assertTrue { changedDrawingBoard.tickets[3].isSelected }
    }

    @Test
    fun `당첨 티켓에는 리워드 이름과 리워드 카테고리 정보가 있다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoardRewardExistsIndex3()

        // when
        val winningTicket = drawingBoard.tickets[3] as Ticket.Winning

        // then
        assertEquals(DrawingBoardFixtureFactory.REWARD_NAME, winningTicket.rewardName)
        assertEquals(DrawingBoardFixtureFactory.REWARD_CATEGORY, winningTicket.rewardCategory)
    }

    @Test
    fun `이미 뽑힌 곳을 뽑으면 오류가 발생한다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()
        val drawingResult = drawingBoard.getDrawingResult(3)
        val changedDrawingBoard = drawingResult.changedDrawingBoard

        // when, then
        assertThrows<AlreadySelectedTicketException> {
            changedDrawingBoard.getDrawingResult(3)
        }
    }

    @Test
    fun `이미 전부 추첨 완료된 보드에서 추첨하면 오류가 발생한다`() {
        // given
        val drawingBoard = DrawingBoardFixtureFactory.createAllSelectedDrawingBoard()

        // when, then
        assertThrows<AlreadySelectedTicketException> { drawingBoard.getDrawingResult(3) }
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
                    val drawingResult = drawingBoard.getDrawingResult(selectedNumber)

                    if (drawingResult is DrawingResult.Winner) 1 else 0
                }.sum()

        val expectedProbability =
            DrawingBoardFixtureFactory.totalRewardCount.toDouble() / DrawingBoardFixtureFactory.SURVEY_PARTICIPANT_COUNT * 100
        val realProbability = winCount.toDouble() / loopCount * 100

        // then
        println("기대 확률 : $expectedProbability%")
        println("실제 확률 : $realProbability%")
        assertTrue(
            (expectedProbability - realProbability).absoluteValue <= 1,
        )
    }
}
