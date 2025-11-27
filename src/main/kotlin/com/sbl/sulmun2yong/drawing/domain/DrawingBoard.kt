package com.sbl.sulmun2yong.drawing.domain

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.domain.ticket.TicketEntity
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import java.util.UUID

class DrawingBoard(
    val id: UUID,
    val surveyId: UUID,
    val ticketEntities: List<TicketEntity>,
) {
    val selectedTicketCount: Int
    val remainingTicketCount: Int

    init {
        selectedTicketCount = calcSelectedTicketCount()
        remainingTicketCount = this.ticketEntities.size - selectedTicketCount
    }

    fun getDrawingResult(selectedIndex: Int): DrawingResult {
        validateOutOfTicket()

        val selectedTicket = this.ticketEntities[selectedIndex]
        validateTicketIsSelected(selectedTicket)

        val changedDrawingBoard = getChangedDrawingBoard(selectedIndex)
        return when (selectedTicket) {
            is TicketEntity.Winning ->
                DrawingResult.Winner(
                    changedDrawingBoard = changedDrawingBoard,
                    rewardName = selectedTicket.rewardName,
                )

            is TicketEntity.NonWinning ->
                DrawingResult.NonWinner(
                    changedDrawingBoard = changedDrawingBoard,
                )
        }
    }

    private fun validateOutOfTicket() {
        if (remainingTicketCount <= 0) {
            throw AlreadySelectedTicketException()
        }
    }

    private fun validateTicketIsSelected(selectedTicketEntity: TicketEntity) {
        if (selectedTicketEntity.isSelected) {
            throw AlreadySelectedTicketException()
        }
    }

    private fun getChangedDrawingBoard(selectedIndex: Int): DrawingBoard =
        DrawingBoard(
            id = this.id,
            surveyId = this.surveyId,
            ticketEntities = deepCopyTicketsWithChangeSelectedTrue(selectedIndex),
        )

    private fun deepCopyTicketsWithChangeSelectedTrue(selectedIndex: Int): List<TicketEntity> {
        val copiedTicketEntities = mutableListOf<TicketEntity>()
        this.ticketEntities.forEachIndexed { index, ticket ->
            copiedTicketEntities.add(
                if (index == selectedIndex) {
                    when (ticket) {
                        is TicketEntity.Winning -> ticket.copy(isSelected = true)
                        is TicketEntity.NonWinning,
                        -> ticket.copy(isSelected = true)
                    }
                } else {
                    ticket
                },
            )
        }

        return copiedTicketEntities.toList()
    }

    private fun calcSelectedTicketCount(): Int = this.ticketEntities.count { it.isSelected }

    companion object {
        fun create(
            surveyId: UUID,
            boardSize: Int,
            rewards: List<Reward>,
        ): DrawingBoard {
            val tickets =
                createTickets(
                    rewards = rewards,
                    maxTicketCount = boardSize,
                )
            return DrawingBoard(
                id = UUID.randomUUID(),
                surveyId = surveyId,
                ticketEntities = tickets,
            )
        }

        private fun createTickets(
            rewards: List<Reward>,
            maxTicketCount: Int,
        ): List<TicketEntity> {
            val ticketEntities = mutableListOf<TicketEntity>()
            rewards.map { reward ->
                repeat(reward.count) {
                    ticketEntities.add(
                        TicketEntity.Winning.create(
                            rewardName = reward.name,
                            rewardCategory = reward.category,
                        ),
                    )
                    require(ticketEntities.size <= maxTicketCount) { throw InvalidDrawingBoardException() }
                }
            }

            repeat(maxTicketCount - ticketEntities.size) {
                ticketEntities.add(TicketEntity.NonWinning.create())
            }
            ticketEntities.shuffle()

            return ticketEntities.toList()
        }
    }
}
