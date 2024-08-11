package com.sbl.sulmun2yong.fixture.drawing

import com.sbl.sulmun2yong.drawing.domain.DrawingBoard
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.survey.domain.Reward
import java.util.UUID

object DrawingBoardFixtureFactory {
    const val SURVEY_PARTICIPANT_COUNT = 200
    const val REWARD_NAME = "테스트 아메리카노"
    const val REWARD_CATEGORY = "음료"

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
    fun createAllSelectedDrawingBoard(): DrawingBoard {
        val rewards = rewards

        return DrawingBoard(
            id = UUID.randomUUID(),
            surveyId = UUID.randomUUID(),
            tickets = createAllSelectedTickets(rewards, SURVEY_PARTICIPANT_COUNT),
        )
    }

    // 3번에 리워드가 있는 보드
    fun createDrawingBoardRewardExistsIndex3(): DrawingBoard {
        val rewards = rewards

        return DrawingBoard(
            id = UUID.randomUUID(),
            surveyId = UUID.randomUUID(),
            tickets = createTicketsRewardExistsIndex3(rewards, SURVEY_PARTICIPANT_COUNT),
        )
    }

    // 3번에 리워드가 없는 보드
    fun createDrawingBoardRewardNotExistsIndex3(): DrawingBoard {
        val rewards = rewards

        return DrawingBoard(
            id = UUID.randomUUID(),
            surveyId = UUID.randomUUID(),
            tickets = createTicketsRewardNotExistsIndex3(rewards, SURVEY_PARTICIPANT_COUNT),
        )
    }

    // 사이즈가 0인 보드
    fun createEmptyDrawingBoard() =
        DrawingBoard.create(
            surveyId = UUID.randomUUID(),
            boardSize = EMPTY_SURVEY_PARTICIPANT_COUNT,
            rewards = emptyList(),
        )

    // 3번에 꽝 티켓이 없는 티켓 리스트 만들기
    private fun createTicketsRewardNotExistsIndex3(
        rewards: List<Reward>,
        maxTicketCount: Int,
    ): List<Ticket> {
        val tickets = mutableListOf<Ticket>()
        rewards.map { reward ->
            repeat(reward.count) {
                tickets.add(
                    Ticket.Winning.create(
                        rewardName = reward.name,
                        rewardCategory = reward.category,
                    ),
                )
                require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
            }
        }

        repeat(maxTicketCount - tickets.size) {
            tickets.add(Ticket.NonWinning(isSelected = true))
        }
        tickets.shuffle()

        tickets[3] =
            Ticket.NonWinning.create()

        return tickets
    }

    // 3번에 당첨 티켓이 있는 티켓 리스트 만들기
    private fun createTicketsRewardExistsIndex3(
        rewards: List<Reward>,
        maxTicketCount: Int,
    ): List<Ticket> {
        val tickets = mutableListOf<Ticket>()
        rewards.map { reward ->
            repeat(reward.count) {
                tickets.add(
                    Ticket.Winning.create(
                        rewardName = reward.name,
                        rewardCategory = reward.category,
                    ),
                )
                require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
            }
        }

        repeat(maxTicketCount - tickets.size) {
            tickets.add(Ticket.NonWinning(isSelected = true))
        }
        tickets.shuffle()

        tickets[3] =
            Ticket.Winning.create(
                rewardName = REWARD_NAME,
                rewardCategory = REWARD_CATEGORY,
            )

        return tickets
    }

    // 모두 선택된 티켓 리스트 만들기
    private fun createAllSelectedTickets(
        rewards: List<Reward>,
        maxTicketCount: Int,
    ): List<Ticket> {
        val tickets = mutableListOf<Ticket>()
        rewards.map { reward ->
            repeat(reward.count) {
                tickets.add(
                    Ticket.Winning(
                        rewardName = reward.name,
                        rewardCategory = reward.category,
                        isSelected = true,
                    ),
                )
                require(tickets.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
            }
        }

        repeat(maxTicketCount - tickets.size) {
            tickets.add(Ticket.NonWinning(isSelected = true))
        }
        tickets.shuffle()

        return tickets
    }
}
