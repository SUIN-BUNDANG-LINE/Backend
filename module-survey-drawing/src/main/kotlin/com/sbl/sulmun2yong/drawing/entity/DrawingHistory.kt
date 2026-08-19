package com.sbl.sulmun2yong.drawing.entity

import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.global.converter.EncryptedPhoneNumberConverter
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.entity.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.util.UUID

@Entity
@Table(name = "drawing_histories")
class DrawingHistory(
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val participantId: UUID,
    @Convert(converter = EncryptedPhoneNumberConverter::class)
    @Column(nullable = false)
    val phoneNumber: PhoneNumber,
    @Column(nullable = false, columnDefinition = "BINARY(16)")
    val surveyId: UUID,
    @Column(nullable = false)
    val selectedTicketIndex: Int,
    // ticket 정보 평탄화 (Sealed Class → 개별 컬럼)
    @Column(nullable = false, length = 20)
    val ticketType: String,
    val rewardName: String?,
    val rewardCategory: String?,
) : BaseTimeEntity() {
    @get:Transient
    val ticket: Ticket
        get() =
            when (ticketType) {
                "WINNING" ->
                    Ticket.Winning(
                        rewardName = rewardName!!,
                        rewardCategory = rewardCategory!!,
                        isSelected = true,
                    )
                else -> Ticket.NonWinning(isSelected = true)
            }

    companion object {
        fun create(
            participantId: UUID,
            phoneNumber: PhoneNumber,
            surveyId: UUID,
            selectedTicketIndex: Int,
            ticket: Ticket,
        ): DrawingHistory {
            val (type, name, category) =
                when (ticket) {
                    is Ticket.Winning -> Triple("WINNING", ticket.rewardName, ticket.rewardCategory)
                    is Ticket.NonWinning -> Triple("NON_WINNING", null, null)
                }
            return DrawingHistory(
                id = UUID.randomUUID(),
                participantId = participantId,
                phoneNumber = phoneNumber,
                surveyId = surveyId,
                selectedTicketIndex = selectedTicketIndex,
                ticketType = type,
                rewardName = name,
                rewardCategory = category,
            )
        }
    }
}
