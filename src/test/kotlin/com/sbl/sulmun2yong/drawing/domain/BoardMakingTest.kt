package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.survey.domain.Reward
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class BoardMakingTest {
    @Test
    fun `추첨 보드를 만든다`() {
        // given
        val id = UUID.randomUUID()
        val surveyParticipantCount = 200
        val rewards =
            listOf(
                Reward(UUID.randomUUID(), "아메리카노", "커피", 3),
                Reward(UUID.randomUUID(), "카페라떼", "커피", 2),
                Reward(UUID.randomUUID(), "햄버거", "음식", 4),
            )

        // when
        DrawingBoard.create(
            id = id,
            size = surveyParticipantCount,
            ticketManager = TicketManager.create(rewards = rewards, maxCount = surveyParticipantCount),
        )

        // then
    }

    @Test
    fun `추첨보드를 만들 때 목표 설문조사자 수 이상으로 리워드를 넣으면 에러가 발생한다`() {
        // given
        val surveyParticipantCount = 200
        val tooManyReward =
            listOf(
                Reward(UUID.randomUUID(), "아메리카노", "커피", 100),
                Reward(UUID.randomUUID(), "카페라떼", "커피", 100),
                Reward(UUID.randomUUID(), "햄버거", "음식", 100),
            )

        // when

        // then
        assertThrows<InvalidDrawingBoardException> {
            DrawingBoard.create(
                id = UUID.randomUUID(),
                size = surveyParticipantCount,
                ticketManager =
                    TicketManager.create(
                        rewards = tooManyReward,
                        maxCount = surveyParticipantCount,
                    ),
            )
        }
        // printTickets(drawingBoard)
    }

    @Test
    fun `마감후 남은 리워드를 비율 추첨으로 재추첨한다`() {
    }
//
//    private fun printTickets(drawingBoard: DrawingBoard) {
//        for (ticket in drawingBoard.tickets) {
//            var wordCount = 0
//            if (ticket.rewardInfo != null) {
//                print("name: ${ticket.rewardInfo!!.name} ")
//            } else {
//                print("꽝 ")
//            }
//            wordCount++
//            if (wordCount % 10 == 0) {
//                println()
//                wordCount = 0
//            }
//        }
//    }
}
