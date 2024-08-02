package com.sbl.sulmun2yong.user.service

import com.sbl.sulmun2yong.drawing.adapter.DrawingBoardAdapter
import com.sbl.sulmun2yong.drawing.adapter.DrawingHistoryAdapter
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupListResponse
import com.sbl.sulmun2yong.drawing.dto.response.DrawingHistoryGroupResponse
import com.sbl.sulmun2yong.user.dto.response.UserSessionsResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdminService(
    val drawingHistoryAdapter: DrawingHistoryAdapter,
    val drawingBoardAdapter: DrawingBoardAdapter,
) {
    fun getAllLoggedInUsers(customOAuth2Users: List<Any>): UserSessionsResponse = UserSessionsResponse(customOAuth2Users)

    fun getDrawingHistory(
        surveyId: UUID,
        isWinnerOnly: Boolean,
    ): DrawingHistoryGroupResponse {
        val drawingHistoryGroup = drawingHistoryAdapter.getBySurveyId(surveyId, isWinnerOnly)
        return DrawingHistoryGroupResponse.of(drawingHistoryGroup)
    }

    fun getDrawingHistoryList(isWinnerOnly: Boolean): DrawingHistoryGroupListResponse {
        val drawingHistoryGroupList = drawingHistoryAdapter.getDrawingHistoryGroupList(isWinnerOnly)
        return DrawingHistoryGroupListResponse.of(drawingHistoryGroupList)
    }
}
