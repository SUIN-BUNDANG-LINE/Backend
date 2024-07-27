package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory.createDrawingBoard
import com.sbl.sulmun2yong.fixture.drawing.DrawingBoardFixtureFactory.createEmptyDrawingBoard
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class BoardMakingTest {
    @Test
    fun `드로잉 보드를 출력한다`() {
        val drawingBoard = createDrawingBoard()
        println(drawingBoard)
    }

    @Test
    fun `사이즈가 0인 보드를 출력한다`() {
        // given
        val emptyRewardBoard = createEmptyDrawingBoard()

        println(emptyRewardBoard)
    }

    @Test
    fun `추첨 보드를 만든다`() {
        // given
        val rewardBoard = createDrawingBoard()

        // when, then
        with(rewardBoard) {
            assertEquals(id, this.id)
            assertEquals(DrawingBoardFixtureFactory.SURVEY_PARTICIPANT_COUNT, this.tickets.size)
        }
    }

    @Test
    fun `추첨보드를 만들 때 목표 설문조사자 수 이상으로 리워드를 넣으면 에러가 발생한다`() {
        // given
        val surveyParticipantCount = 200

        // when
        val tooManyReward =
            arrayOf(
                Reward(UUID.randomUUID(), "아메리카노", "커피", 100),
                Reward(UUID.randomUUID(), "카페라떼", "커피", 100),
                Reward(UUID.randomUUID(), "햄버거", "음식", 100),
            )

        // then
        assertThrows<InvalidDrawingBoardException> {
            DrawingBoard.create(
                id = UUID.randomUUID(),
                surveyId = UUID.randomUUID(),
                boardSize = surveyParticipantCount,
                rewards = tooManyReward,
            )
        }
    }
}
