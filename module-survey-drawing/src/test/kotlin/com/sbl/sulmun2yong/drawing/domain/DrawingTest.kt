package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

/**
 * 보드가 스스로 아는 것만 검증한다.
 *
 * "이미 뽑힌 칸인가"·"판이 다 찼는가"는 조건부 UPDATE 가 판정한다 — 읽고 나서 판단하면 그 사이
 * 다른 요청이 끼어들 수 있어 앱 메모리에서는 옳게 답할 수 없다. 그 규칙들은 DB 가 필요한
 * 검증 대상이라 여기서 다루지 않는다.
 */
class DrawingTest {
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
    fun `보드를 만들면 경품 수만큼 당첨 티켓이 놓이고 나머지는 전부 꽝이다`() {
        // when
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()

        // then
        assertEquals(DrawingBoardFixtureFactory.SURVEY_PARTICIPANT_COUNT, drawingBoard.tickets.size)
        assertEquals(
            DrawingBoardFixtureFactory.totalRewardCount,
            drawingBoard.tickets.count { it is Ticket.Winning },
        )
    }

    @Test
    fun `새로 만든 보드는 아무도 뽑지 않아 잔여가 전체와 같다`() {
        // when
        val drawingBoard = DrawingBoardFixtureFactory.createDrawingBoard()

        // then
        assertEquals(0, drawingBoard.selectedTicketCount)
        assertEquals(DrawingBoardFixtureFactory.SURVEY_PARTICIPANT_COUNT, drawingBoard.remainingTicketCount)
    }

    @Test
    fun `전부 뽑힌 보드는 잔여가 0이다`() {
        // when
        val drawingBoard = DrawingBoardFixtureFactory.createAllSelectedDrawingBoard()

        // then
        assertEquals(DrawingBoardFixtureFactory.SURVEY_PARTICIPANT_COUNT, drawingBoard.selectedTicketCount)
        assertEquals(0, drawingBoard.remainingTicketCount)
    }

    @Test
    fun `보드를 loopCount 번 만들었을 때 특정 자리가 당첨일 기대 확률과 실제 확률 차가 1 미만이다`() {
        // given — 당첨 확률은 뽑는 행위가 아니라 보드를 만들 때의 섞기가 정한다
        val selectedNumber = 3
        val loopCount = 500000

        // when
        val winCount =
            (1..loopCount).count {
                DrawingBoardFixtureFactory.createDrawingBoard().tickets[selectedNumber] is Ticket.Winning
            }

        val expectedProbability =
            DrawingBoardFixtureFactory.totalRewardCount.toDouble() / DrawingBoardFixtureFactory.SURVEY_PARTICIPANT_COUNT * 100
        val realProbability = winCount.toDouble() / loopCount * 100

        // then
        println("기대 확률 : $expectedProbability%")
        println("실제 확률 : $realProbability%")
        assertTrue((expectedProbability - realProbability).absoluteValue <= 1)
    }
}
