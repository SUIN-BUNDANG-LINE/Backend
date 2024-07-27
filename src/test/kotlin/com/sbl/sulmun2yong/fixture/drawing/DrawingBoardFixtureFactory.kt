package com.sbl.sulmun2yong.fixture.drawing

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.Reward
import com.sbl.sulmun2yong.drawing.domain.ticket.NonWinningTicket
import com.sbl.sulmun2yong.drawing.domain.ticket.TicketFactory
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import java.util.UUID

object DrawingBoardFixtureFactory {
    const val SURVEY_PARTICIPANT_COUNT = 200
    private const val EMPTY_SURVEY_PARTICIPANT_COUNT = 0
    private val rewards =
        arrayOf(
            Reward(UUID.randomUUID(), "아메리카노", "커피", 3),
            Reward(UUID.randomUUID(), "카페라떼", "커피", 2),
            Reward(UUID.randomUUID(), "햄버거", "음식", 2),
        )

    val totalRewardCount = rewards.sumOf { it.count }

    // 적당히 리워드가 있는 보드
    fun createDrawingBoard(): DrawingBoard {
        val rewards = copyRewards()

        return DrawingBoard.create(
            id = UUID.randomUUID(),
            boardSize = SURVEY_PARTICIPANT_COUNT,
            rewards = rewards,
        )
    }

    // 리워드가 없는 보드
    fun createEmptyDrawingBoard() =
        DrawingBoard.create(
            id = UUID.randomUUID(),
            boardSize = EMPTY_SURVEY_PARTICIPANT_COUNT,
            rewards = emptyArray(),
        )

    // 이미 모든 티켓이 선택된 보드
    fun createAllSelectedRewardDrawingBoard() =
        DrawingBoard(
            id = UUID.randomUUID(),
            selectedTicketCount = SURVEY_PARTICIPANT_COUNT,
            tickets =
                TicketFactory.createTickets(
                    rewards = emptyArray(),
                    maxTicketCount = SURVEY_PARTICIPANT_COUNT,
                ),
        )

    // 3번에 리워드가 있는 보드
    fun createRewardAtIndex3DrawingBoard(): DrawingBoard {
        val rewards = copyRewards()

        val drawingBoard =
            DrawingBoard.create(
                id = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            )

        drawingBoard.tickets[3] = WinningTicket("테스트 아이스 아메리카노")

        return drawingBoard
    }

    // 3번에 리워드가 있는 보드 + 티켓이 하나 남은 보드
    fun createRewardAtIndex3DrawingBoardRemainOne(): DrawingBoard {
        val rewards = copyRewards()

        val drawingBoard =
            DrawingBoard.create(
                id = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            )

        drawingBoard.selectedTicketCount = SURVEY_PARTICIPANT_COUNT - 1
        drawingBoard.tickets[3] = WinningTicket("테스트 아이스 아메리카노")

        return drawingBoard
    }

    // 3번에 리워드가 없는 보드 + 티켓이 하나 남은 보드
    fun createNoRewardAtIndex3DrawingBoardRemainOne(): DrawingBoard {
        val rewards = copyRewards()

        val drawingBoard =
            DrawingBoard.create(
                id = UUID.randomUUID(),
                boardSize = SURVEY_PARTICIPANT_COUNT,
                rewards = rewards,
            )

        drawingBoard.selectedTicketCount = SURVEY_PARTICIPANT_COUNT - 1
        drawingBoard.tickets[3] = NonWinningTicket()

        return drawingBoard
    }

    private fun copyRewards(): Array<Reward> = Array(rewards.size) { index -> rewards[index].copy() }
}
