package com.sbl.sulmun2yong.drawing.adapter

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.DrawingHistoryGroup
import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryDocument
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.data.PhoneNumber
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DrawingHistoryAdapter(
    private val drawingHistoryRepository: DrawingHistoryRepository,
) {
    fun insert(drawingHistory: DrawingHistory) {
        drawingHistoryRepository.insert(DrawingHistoryDocument.of(drawingHistory))
    }

    fun findBySurveyIdAndParticipantIdOrPhoneNumber(
        surveyId: UUID,
        participantId: UUID,
        phoneNumber: PhoneNumber,
    ): DrawingHistory? =
        drawingHistoryRepository
            .findBySurveyIdAndParticipantIdOrPhoneNumber(surveyId, participantId, phoneNumber.value)
            .map { it.toDomain() }
            .orElse(null)

    fun getBySurveyId(
        surveyId: UUID,
        isWinnerOnly: Boolean,
    ): DrawingHistoryGroup {
        val dto =
            when (isWinnerOnly) {
                true -> drawingHistoryRepository.findBySurveyIdForWinner(surveyId)
                false -> drawingHistoryRepository.findBySurveyId(surveyId)
            }
        if (!dto.isPresent) {
            return DrawingHistoryGroup(
                surveyId = surveyId,
                count = 0,
                histories = emptyList(),
            )
        }
        return DrawingHistoryGroup(
            surveyId = dto.get().id,
            count = dto.get().count,
            histories = dto.get().items.map { it.toDomain() },
        )
    }

    fun getDrawingHistoryGroupList(isWinnerOnly: Boolean): List<DrawingHistoryGroup> {
        val documentsGroupedBySurveyId =
            when (isWinnerOnly) {
                true -> drawingHistoryRepository.findGroupedBySurveyIdForWinner()
                false -> drawingHistoryRepository.findGroupedSurveyId()
            }
        return documentsGroupedBySurveyId.map { it ->
            DrawingHistoryGroup(
                surveyId = it.id,
                count = it.count,
                histories = it.items.map { it.toDomain() },
            )
        }
    }
}
