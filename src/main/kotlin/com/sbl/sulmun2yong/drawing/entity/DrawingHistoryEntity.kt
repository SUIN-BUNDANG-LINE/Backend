package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.global.converter.EncryptedStringConverter
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "drawing_histories")
class DrawingHistoryEntity(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val participantId: UUID,
    @Convert(converter = EncryptedStringConverter::class)
    @Column(nullable = false)
    val phoneNumber: String,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @Column(nullable = false)
    val selectedTicketIndex: Int,
    // ticket 정보 평탄화 (Sealed Class → 개별 컬럼)
    // "WINNING" | "NON_WINNING"
    @Column(nullable = false, length = 20)
    val ticketType: String,
    val rewardName: String?,
    val rewardCategory: String?,
) : BaseTimeEntity() {
    fun toDomain(): DrawingHistory {
        val ticket =
            when (ticketType) {
                "WINNING" ->
                    Ticket.Winning(
                        rewardName = rewardName!!,
                        rewardCategory = rewardCategory!!,
                        isSelected = true,
                    )
                else -> Ticket.NonWinning(isSelected = true)
            }
        return DrawingHistory(
            id = id,
            participantId = participantId,
            phoneNumber = PhoneNumber.createWithNonNullable(phoneNumber),
            surveyId = surveyId,
            selectedTicketIndex = selectedTicketIndex,
            ticket = ticket,
        )
    }

    companion object {
        fun from(drawingHistory: DrawingHistory): DrawingHistoryEntity {
            val (ticketType, rewardName, rewardCategory) =
                when (val ticket = drawingHistory.ticket) {
                    is Ticket.Winning -> Triple("WINNING", ticket.rewardName, ticket.rewardCategory)
                    is Ticket.NonWinning -> Triple("NON_WINNING", null, null)
                }
            return DrawingHistoryEntity(
                id = drawingHistory.id,
                participantId = drawingHistory.participantId,
                phoneNumber = drawingHistory.phoneNumber.value,
                surveyId = drawingHistory.surveyId,
                selectedTicketIndex = drawingHistory.selectedTicketIndex,
                ticketType = ticketType,
                rewardName = rewardName,
                rewardCategory = rewardCategory,
            )
        }
    }
}
