package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.drawing.exception.InvalidDrawingBoardException
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import com.sbl.sulmun2yong.survey.domain.reward.Reward
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "drawing_boards")
class DrawingBoard(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    // 결제 수명 주기 - 보드가 곧 "산 물건"이라 대금 상태를 보드가 진다.
    // PENDING_PAYMENT 보드의 존재 = 설문 수정 잠금(경품 스냅숏 동결).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DrawingBoardStatus = DrawingBoardStatus.PENDING_PAYMENT,
    @OneToMany(mappedBy = "drawingBoard", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("ticketIndex ASC")
    val ticketEntities: MutableList<TicketEntity> = mutableListOf(),
) : BaseTimeEntity() {
    // 대금 확정 - 설문 활성화와 같은 tx 에서 호출된다. 재전달 멱등은 호출자의 상태 가드 몫.
    fun activate() {
        this.status = DrawingBoardStatus.ACTIVE
    }

    @get:Transient
    val tickets: List<Ticket>
        get() = ticketEntities.map { it.toDomain() }

    @get:Transient
    val selectedTicketCount: Int
        get() = ticketEntities.count { it.isSelected }

    @get:Transient
    val remainingTicketCount: Int
        get() = ticketEntities.size - selectedTicketCount

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
