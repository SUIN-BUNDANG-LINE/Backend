package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "tickets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
abstract class TicketEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drawing_board_id", nullable = false)
    open var drawingBoard: DrawingBoard? = null

    @Column(nullable = false)
    open var ticketIndex: Int = 0

    @Column(nullable = false)
    open var isSelected: Boolean = false

    abstract fun toDomain(): Ticket

    companion object {
        fun from(
            ticket: Ticket,
            index: Int,
            drawingBoard: DrawingBoard,
        ): TicketEntity =
            when (ticket) {
                is Ticket.Winning ->
                    WinningTicketEntity().apply {
                        this.drawingBoard = drawingBoard
                        this.ticketIndex = index
                        this.isSelected = ticket.isSelected
                        this.rewardName = ticket.rewardName
                        this.rewardCategory = ticket.rewardCategory
                    }
                is Ticket.NonWinning ->
                    NonWinningTicketEntity().apply {
                        this.drawingBoard = drawingBoard
                        this.ticketIndex = index
                        this.isSelected = ticket.isSelected
                    }
            }
    }
}

@Entity
@DiscriminatorValue("WINNING")
class WinningTicketEntity : TicketEntity() {
    @Column
    var rewardName: String? = null

    @Column
    var rewardCategory: String? = null

    override fun toDomain() =
        Ticket.Winning(
            rewardName = rewardName!!,
            rewardCategory = rewardCategory!!,
            isSelected = isSelected,
        )
}

@Entity
@DiscriminatorValue("NON_WINNING")
class NonWinningTicketEntity : TicketEntity() {
    override fun toDomain() = Ticket.NonWinning(isSelected = isSelected)
}
