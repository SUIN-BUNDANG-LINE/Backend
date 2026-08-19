package com.sbl.sulmun2yong.drawing.dto.response

import com.sbl.sulmun2yong.drawing.domain.DrawingHistoryGroup
import java.util.UUID

class DrawingHistoryGroupResponse(
    val surveyId: UUID,
    val count: Int,
    val histories: List<DrawingHistoryResponse>,
) {
    companion object {
        fun of(drawingHistoryGroup: DrawingHistoryGroup) =
            DrawingHistoryGroupResponse(
                surveyId = drawingHistoryGroup.surveyId,
                count = drawingHistoryGroup.count,
                histories =
                    drawingHistoryGroup.histories.map {
                        DrawingHistoryResponse.of(it)
                    },
            )
    }
}
