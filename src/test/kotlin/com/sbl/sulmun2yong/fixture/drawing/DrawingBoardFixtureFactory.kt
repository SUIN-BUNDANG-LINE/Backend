package com.sbl.sulmun2yong.fixture.drawing

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.Reward
import com.sbl.sulmun2yong.drawing.domain.ticket.NonWinningTicket
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import java.util.UUID

object DrawingBoardFixtureFactory {
    const val SURVEY_PARTICIPANT_COUNT = 200
    private const val EMPTY_SURVEY_PARTICIPANT_COUNT = 0
    private val rewards =
        arrayOf(
            Reward("아메리카노", "커피", 3),
            Reward("카페라떼", "커피", 2),
            Reward("햄버거", "음식", 2),
        )

    val totalRewardCount = rewards.sumOf { it.count }

    const val REWARD_NAME = "테스트 아메리카노"

    private fun createWinningTicket() =
        WinningTicket.create(
            rewardName = "테스트 아메리카노",
            rewardCategory = "테스트 커피",
        )

    private fun createNonWinningTicket() = NonWinningTicket.create()

    // 적당히 리워드가 있는 보드
    fun createDrawingBoard(): DrawingBoard {
        val rewards = rewards

        return DrawingBoard.create(
            surveyId = UUID.randomUUID(),
            boardSize = SURVEY_PARTICIPANT_COUNT,
            rewards = rewards,
        )
    }

    // 사이즈가 0인 보드
    fun createEmptyDrawingBoard() =
        DrawingBoard.create(
            surveyId = UUID.randomUUID(),
            boardSize = EMPTY_SURVEY_PARTICIPANT_COUNT,
            rewards = emptyArray(),
        )

    // 리워드가 없는 보드
    fun createNoRewardDrawingBoard() =
        DrawingBoard.create(
            surveyId = UUID.randomUUID(),
            boardSize = SURVEY_PARTICIPANT_COUNT,
            rewards = emptyArray(),
        )

    // 이미 모든 티켓이 선택된 보드
    fun createAllSelectedRewardDrawingBoard() =
        DrawingBoard
            .create(
                surveyId = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            ).apply {
                selectedTicketCount = SURVEY_PARTICIPANT_COUNT
                tickets.forEach { it.isSelected = true }
            }

    // 3번에 리워드가 있는 보드
    fun createRewardAtIndex3DrawingBoard(): DrawingBoard {
        val rewards = rewards

        val drawingBoard =
            DrawingBoard.create(
                surveyId = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            )

        drawingBoard.tickets[3] = createWinningTicket()

        return drawingBoard
    }

    // 3번이 이미 선택된 보드
    fun createSelectedIndex3DrawingBoard(): DrawingBoard {
        val rewards = rewards

        val drawingBoard =
            DrawingBoard.create(
                surveyId = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            )

        drawingBoard.tickets[3].isSelected = true

        return drawingBoard
    }

    // 3번에 리워드가 있는 보드 + 티켓이 하나 남은 보드
    fun createRewardAtIndex3DrawingBoardRemainOne(): DrawingBoard {
        val rewards = rewards

        val drawingBoard =
            DrawingBoard.create(
                surveyId = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            )

        drawingBoard.selectedTicketCount = SURVEY_PARTICIPANT_COUNT - 1
        drawingBoard.tickets[3] = createWinningTicket()

        return drawingBoard
    }

    // 3번에 리워드가 없는 보드 + 티켓이 하나 남은 보드
    fun createNoRewardAtIndex3DrawingBoardRemainOne(): DrawingBoard {
        val rewards = rewards

        val drawingBoard =
            DrawingBoard.create(
                surveyId = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            )

        drawingBoard.selectedTicketCount = SURVEY_PARTICIPANT_COUNT - 1
        drawingBoard.tickets[3] = createNonWinningTicket()

        return drawingBoard
    }
}
