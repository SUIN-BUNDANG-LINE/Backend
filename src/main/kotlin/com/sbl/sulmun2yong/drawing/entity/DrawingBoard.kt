package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.drawingResult.DrawingResult
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.exception.AlreadySelectedTicketException
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.util.UUID

@Entity
@Table(name = "drawing_boards")
class DrawingBoard(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @OneToMany(mappedBy = "drawingBoard", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("ticketIndex ASC")
    val ticketEntities: MutableList<TicketEntity> = mutableListOf(),
) : BaseTimeEntity() {
    @get:Transient
    val tickets: List<Ticket>
        get() = ticketEntities.map { it.toDomain() }

    @get:Transient
    val selectedTicketCount: Int
        get() = ticketEntities.count { it.isSelected }

    @get:Transient
    val remainingTicketCount: Int
        get() = ticketEntities.size - selectedTicketCount

    fun getDrawingResult(selectedIndex: Int): DrawingResult {
        validateOutOfTicket()
        val selectedTicket = tickets[selectedIndex]
        validateTicketIsSelected(selectedTicket)

        val changedDrawingBoard = getChangedDrawingBoard(selectedIndex)
        return when (selectedTicket) {
            is Ticket.Winning ->
                DrawingResult.Winner(
                    changedDrawingBoard = changedDrawingBoard,
                    rewardName = selectedTicket.rewardName,
                )
            is Ticket.NonWinning ->
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

    private fun validateTicketIsSelected(selectedTicket: Ticket) {
        if (selectedTicket.isSelected) {
            throw AlreadySelectedTicketException()
        }
    }

    private fun getChangedDrawingBoard(selectedIndex: Int): DrawingBoard =
        fromTickets(
            id = this.id,
            surveyId = this.surveyId,
            tickets = deepCopyTicketsWithChangeSelectedTrue(selectedIndex),
        )

    private fun deepCopyTicketsWithChangeSelectedTrue(selectedIndex: Int): List<Ticket> =
        tickets.mapIndexed { index, ticket ->
            if (index == selectedIndex) {
                when (ticket) {
                    is Ticket.Winning -> ticket.copy(isSelected = true)
                    is Ticket.NonWinning -> ticket.copy(isSelected = true)
                }
            } else {
                ticket
            }
        }

    companion object {
        fun create(
            surveyId: UUID,
            boardSize: Int,
            rewards: List<Reward>,
        ): DrawingBoard =
            fromTickets(
                id = UUID.randomUUID(),
                surveyId = surveyId,
                tickets = createTickets(rewards, boardSize),
            )

        fun fromTickets(
            id: UUID,
            surveyId: UUID,
            tickets: List<Ticket>,
        ): DrawingBoard {
            val entity = DrawingBoard(id = id, surveyId = surveyId)
            tickets.forEachIndexed { index, ticket ->
                entity.ticketEntities.add(TicketEntity.from(ticket, index, entity))
            }
            return entity
        }

        private fun createTickets(
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
                tickets.add(Ticket.NonWinning.create())
            }
            tickets.shuffle()
            return tickets.toList()
        }
    }
}
