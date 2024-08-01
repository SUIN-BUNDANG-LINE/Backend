package com.sbl.sulmun2yong.fixture.drawing

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.Reward
import com.sbl.sulmun2yong.drawing.domain.ticket.NonWinningTicket
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.domain.ticket.WinningTicket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import java.util.UUID

object DrawingBoardFixtureFactory {
    const val SURVEY_PARTICIPANT_COUNT = 200
    const val REWARD_NAME = "테스트 아메리카노"

    private const val EMPTY_SURVEY_PARTICIPANT_COUNT = 0
    private val rewards =
        listOf(
            Reward("아메리카노", "커피", 3),
            Reward("카페라떼", "커피", 2),
            Reward("햄버거", "음식", 2),
        )
    val totalRewardCount = rewards.sumOf { it.count }

    // 적당히 리워드가 있는 보드
    fun createDrawingBoard(): DrawingBoard {
        val rewards = rewards

        return DrawingBoard.create(
            surveyId = UUID.randomUUID(),
            boardSize = SURVEY_PARTICIPANT_COUNT,
            rewards = rewards,
        )
    }

    // 모두 추첨 완료된 보드
    fun createFinishedDrawingBoard(): DrawingBoard {
        val rewards = rewards

        return DrawingBoard(
            id = UUID.randomUUID(),
            surveyId = UUID.randomUUID(),
            selectedTicketCount = SURVEY_PARTICIPANT_COUNT,
            tickets = createAllSelectedTickets(rewards, SURVEY_PARTICIPANT_COUNT),
        )
    }

    // 사이즈가 0인 보드
    fun createEmptyDrawingBoard() =
        DrawingBoard.create(
            surveyId = UUID.randomUUID(),
            boardSize = EMPTY_SURVEY_PARTICIPANT_COUNT,
            rewards = emptyList(),
        )

    // 모두 선택된 티켓 만들기
    private fun createAllSelectedTickets(
        rewards: List<Reward>,
        maxTicketCount: Int,
    ): List<Ticket> {
        val tickets = mutableListOf<Ticket>()
        rewards.map { reward ->
            repeat(reward.count) {
                tickets.add(
                    WinningTicket(
                        rewardName = reward.name,
                        rewardCategory = reward.category,
                        isSelected = true,
                    ),
                )
                require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
            }
        }

        repeat(maxTicketCount - tickets.size) {
            tickets.add(NonWinningTicket(isSelected = true))
        }
        tickets.shuffle()

        return tickets
    }
}
