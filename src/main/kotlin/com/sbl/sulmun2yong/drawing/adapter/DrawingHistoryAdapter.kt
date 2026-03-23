package com.sbl.sulmun2yong.drawing.adapter

import com.sbl.sulmun2yong.drawing.domain.DrawingHistory
import com.sbl.sulmun2yong.drawing.domain.DrawingHistoryGroup
import com.sbl.sulmun2yong.drawing.entity.DrawingHistoryEntity
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import com.sbl.sulmun2yong.global.data.PhoneNumber
import com.sbl.sulmun2yong.global.util.EncryptionUtils
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DrawingHistoryAdapter(
    private val drawingHistoryRepository: DrawingHistoryRepository,
    private val encryptionUtils: EncryptionUtils,
) {
    fun insert(drawingHistory: DrawingHistory) {
        drawingHistoryRepository.save(DrawingHistoryEntity.from(drawingHistory))
    }

    fun findBySurveyIdAndParticipantIdOrPhoneNumber(
        surveyId: UUID,
        participantId: UUID,
        phoneNumber: PhoneNumber,
    ): DrawingHistory? =
        drawingHistoryRepository
            .findBySurveyIdAndParticipantIdOrPhoneNumber(
                surveyId,
                participantId,
                encryptionUtils.encrypt(phoneNumber.value),
            ).map { it.toDomain() }
            .orElse(null)

    fun getBySurveyId(
        surveyId: UUID,
        isWinnerOnly: Boolean,
    ): DrawingHistoryGroup {
        val histories =
            when (isWinnerOnly) {
                true -> drawingHistoryRepository.findBySurveyIdForWinner(surveyId)
                false -> drawingHistoryRepository.findBySurveyId(surveyId)
            }
        return DrawingHistoryGroup(
            surveyId = surveyId,
            count = histories.size,
            histories = histories.map { it.toDomain() },
        )
    }

    fun getDrawingHistoryGroupList(isWinnerOnly: Boolean): List<DrawingHistoryGroup> {
        val allHistories =
            when (isWinnerOnly) {
                true -> drawingHistoryRepository.findAllWinners()
                false -> drawingHistoryRepository.findAll()
            }
        return allHistories
            .groupBy { it.surveyId }
            .map { (surveyId, histories) ->
                DrawingHistoryGroup(
                    surveyId = surveyId,
                    count = histories.size,
                    histories = histories.map { it.toDomain() },
                )
            }
    }
}
