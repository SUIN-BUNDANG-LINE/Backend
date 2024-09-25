package com.sbl.sulmun2yong.survey.dto.response

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.DrawingHistoryGroup
import com.sbl.sulmun2yong.drawing.domain.ticket.Ticket
import com.sbl.sulmun2yong.survey.domain.Participant
import java.util.Date
import java.util.UUID

data class ParticipantsInfoListResponse(
    val participants: List<ParticipantInfoResponse>,
    val isImmediateDraw: Boolean,
) {
    data class ParticipantInfoResponse(
        val participantId: UUID,
        val participatedAt: Date,
        val drawInfo: DrawInfoResponse?,
    )

    data class DrawInfoResponse(
        val drawResult: DrawResult,
        val reward: String?,
        val phoneNumber: String?,
    ) {
        companion object {
            fun from(drawingHistory: DrawingHistory?): DrawInfoResponse {
                if (drawingHistory == null) {
                    return DrawInfoResponse(
                        drawResult = DrawResult.BEFORE_DRAW,
                        reward = null,
                        phoneNumber = null,
                    )
                }

                if (drawingHistory.ticket is Ticket.Winning) {
                    return DrawInfoResponse(
                        drawResult = DrawResult.WIN,
                        reward = drawingHistory.ticket.rewardName,
                        phoneNumber = drawingHistory.phoneNumber.value,
                    )
                }

                return DrawInfoResponse(
                    drawResult = DrawResult.LOSE,
                    reward = null,
                    phoneNumber = null,
                )
            }
        }
    }

    enum class DrawResult {
        BEFORE_DRAW,
        WIN,
        LOSE,
    }

    companion object {
        fun of(
            participants: List<Participant>,
            drawingHistories: DrawingHistoryGroup?,
        ): ParticipantsInfoListResponse {
            if (drawingHistories == null) {
                return ParticipantsInfoListResponse(
                    participants =
                        participants.map {
                            ParticipantInfoResponse(
                                participantId = it.id,
                                participatedAt = it.createdAt,
                                drawInfo = null,
                            )
                        },
                    isImmediateDraw = false,
                )
            }

            val drawingHistoryMap = drawingHistories.histories.associateBy { it.participantId }

            return ParticipantsInfoListResponse(
                participants =
                    participants.map {
                        val drawingHistory = drawingHistoryMap[it.id]
                        ParticipantInfoResponse(
                            participantId = it.id,
                            participatedAt = it.createdAt,
                            drawInfo = DrawInfoResponse.from(drawingHistory),
                        )
                    },
                isImmediateDraw = true,
            )
        }
    }
}
