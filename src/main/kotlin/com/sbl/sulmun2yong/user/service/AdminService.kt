package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.drawing.domain.DrawingHistoryGroup
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupListResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupResponse
import com.sbl.sulmun2yong.drawing.repository.DrawingHistoryRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdminService(
    val drawingHistoryRepository: DrawingHistoryRepository,
) {
    fun getDrawingHistory(
        surveyId: UUID,
        isWinnerOnly: Boolean,
    ): DrawingHistoryGroupResponse {
        val histories =
            when (isWinnerOnly) {
                true -> drawingHistoryRepository.findBySurveyIdForWinner(surveyId)
                false -> drawingHistoryRepository.findBySurveyId(surveyId)
            }
        val drawingHistoryGroup = DrawingHistoryGroup(surveyId, histories.size, histories)
        return DrawingHistoryGroupResponse.of(drawingHistoryGroup)
    }

    fun getDrawingHistoryList(isWinnerOnly: Boolean): DrawingHistoryGroupListResponse {
        val allHistories =
            when (isWinnerOnly) {
                true -> drawingHistoryRepository.findAllWinners()
                false -> drawingHistoryRepository.findAll()
            }
        val drawingHistoryGroupList =
            allHistories
                .groupBy { it.surveyId }
                .map { (surveyId, histories) ->
                    DrawingHistoryGroup(surveyId, histories.size, histories)
                }
        return DrawingHistoryGroupListResponse.of(drawingHistoryGroupList)
    }
}
